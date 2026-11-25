package br.ufscar.pescd.service;

import br.ufscar.pescd.dto.WorkPlanForm;
import br.ufscar.pescd.entity.Offer;
import br.ufscar.pescd.entity.OfferStudent;
import br.ufscar.pescd.entity.StatusChangeLog;
import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.entity.WorkPlan;
import br.ufscar.pescd.enums.OfferStatus;
import br.ufscar.pescd.enums.StudentOfferStatus;
import br.ufscar.pescd.enums.UserRole;
import br.ufscar.pescd.repository.DocumentationRepository;
import br.ufscar.pescd.repository.OfferStudentRepository;
import br.ufscar.pescd.repository.ReportRepository;
import br.ufscar.pescd.repository.StatusChangeLogRepository;
import br.ufscar.pescd.repository.UserRepository;
import br.ufscar.pescd.repository.WorkPlanRepository;
import br.ufscar.pescd.storage.SeaweedFsStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class StudentOfferServiceMud2Test {

    private OfferStudentRepository offerStudentRepository;
    private UserRepository userRepository;
    private WorkPlanRepository workPlanRepository;
    private StatusChangeLogRepository statusChangeLogRepository;
    private SeaweedFsStorageService storageService;
    private StudentOfferService service;

    private User student;
    private User supervisor;
    private OfferStudent enrollment;
    private WorkPlan workPlan;
    private WorkPlanForm form;

    @BeforeEach
    public void setUp() {
        offerStudentRepository = mock(OfferStudentRepository.class);
        userRepository = mock(UserRepository.class);
        workPlanRepository = mock(WorkPlanRepository.class);
        statusChangeLogRepository = mock(StatusChangeLogRepository.class);
        storageService = mock(SeaweedFsStorageService.class);

        service = new StudentOfferService(
                offerStudentRepository,
                userRepository,
                workPlanRepository,
                mock(DocumentationRepository.class),
                mock(ReportRepository.class),
                statusChangeLogRepository,
                mock(OfferApiMapper.class),
                storageService
        );

        student = new User();
        student.setId(20L);
        student.setUsername("alunoreprovado");
        student.setRole(UserRole.ALUNO);

        supervisor = new User();
        supervisor.setId(3L);
        supervisor.setRole(UserRole.PROFESSOR);

        Offer offer = new Offer();
        offer.setId(10L);
        offer.setStatus(OfferStatus.EM_ANDAMENTO);

        enrollment = new OfferStudent();
        enrollment.setId(5L);
        enrollment.setOffer(offer);
        enrollment.setStudent(student);
        enrollment.setSupervisor(supervisor);
        enrollment.setStatus(StudentOfferStatus.PLANO_REPROVADO);

        workPlan = new WorkPlan();
        workPlan.setId(7L);
        workPlan.setOfferStudent(enrollment);
        workPlan.setDisciplineCode("OLD");
        workPlan.setDisciplineName("Plano antigo");
        workPlan.setDisciplineCourse("Curso antigo");
        workPlan.setFileName("antigo.pdf");
        workPlan.setContentType("application/pdf");
        workPlan.setFileFid("old-fid");
        workPlan.setSupervisorParecer("Parecer de reprovação anterior");
        workPlan.setSupervisorRejectedAt(LocalDateTime.now().minusDays(1));

        form = new WorkPlanForm();
        form.setDisciplineCode("1000999");
        form.setDisciplineName("Engenharia de Software 2");
        form.setDisciplineCourse("Ciência da Computação");
        form.setSupervisorId(3L);

        MockMultipartFile file = new MockMultipartFile(
                "file", "plano-novo.pdf", "application/pdf", "%PDF-1.4".getBytes());
        form.setFile(file);

        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    public void clearSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void mockResubmissionContext() {
        when(offerStudentRepository.findForUpdateById(5L)).thenReturn(Optional.of(enrollment));
        when(userRepository.findById(3L)).thenReturn(Optional.of(supervisor));
        when(workPlanRepository.findByOfferStudent(enrollment)).thenReturn(Optional.of(workPlan));
        when(storageService.store(any(byte[].class), anyString(), anyString())).thenReturn("new-fid");
    }

    @Test
    public void submitWorkPlanAllowsResubmissionAfterRejectionAndClearsPreviousDecision() {
        mockResubmissionContext();

        service.submitWorkPlan(5L, "alunoreprovado", form);

        assertEquals(StudentOfferStatus.PLANO_ENVIADO, enrollment.getStatus());
        assertEquals(1000999, Integer.parseInt(workPlan.getDisciplineCode()));
        assertEquals("plano-novo.pdf", workPlan.getFileName());
        assertEquals("new-fid", workPlan.getFileFid());
        assertNull(workPlan.getSupervisorParecer());
        assertNull(workPlan.getSupervisorApprovedAt());
        assertNull(workPlan.getSupervisorRejectedAt());

        verify(storageService, never()).delete(any());
        verify(storageService).store(any(byte[].class), anyString(), anyString());
        verify(workPlanRepository).save(workPlan);
        verify(offerStudentRepository).save(enrollment);

        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> {
            sync.afterCommit();
            sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        });
        verify(storageService).delete("old-fid");

        ArgumentCaptor<StatusChangeLog> logCaptor = ArgumentCaptor.forClass(StatusChangeLog.class);
        verify(statusChangeLogRepository).save(logCaptor.capture());
        assertEquals(StudentOfferStatus.PLANO_REPROVADO, logCaptor.getValue().getPreviousStatus());
        assertEquals(StudentOfferStatus.PLANO_ENVIADO, logCaptor.getValue().getNewStatus());
        assertEquals("Plano de trabalho reenviado", logCaptor.getValue().getDescription());
    }

    @Test
    public void keepsOriginalPdfWhenUploadFails() {
        when(offerStudentRepository.findForUpdateById(5L)).thenReturn(Optional.of(enrollment));
        when(userRepository.findById(3L)).thenReturn(Optional.of(supervisor));
        when(workPlanRepository.findByOfferStudent(enrollment)).thenReturn(Optional.of(workPlan));
        when(storageService.store(any(byte[].class), anyString(), anyString()))
                .thenThrow(new IllegalStateException("Falha no armazenamento"));

        assertThrows(IllegalStateException.class,
                () -> service.submitWorkPlan(5L, "alunoreprovado", form));

        assertEquals(StudentOfferStatus.PLANO_REPROVADO, enrollment.getStatus());
        assertEquals("old-fid", workPlan.getFileFid());
        assertEquals("Parecer de reprovação anterior", workPlan.getSupervisorParecer());
        verify(storageService, never()).delete(any());
        verify(workPlanRepository, never()).save(any());
        verifyNoInteractions(statusChangeLogRepository);
    }

    @Test
    public void cleansUpNewPdfWhenDatabaseRollsBack() {
        mockResubmissionContext();
        when(workPlanRepository.save(workPlan)).thenThrow(new IllegalStateException("Falha no banco"));

        assertThrows(IllegalStateException.class,
                () -> service.submitWorkPlan(5L, "alunoreprovado", form));

        TransactionSynchronizationManager.getSynchronizations().forEach(sync ->
                sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(storageService).delete("new-fid");
        verify(storageService, never()).delete("old-fid");
    }

    @Test
    public void submitWorkPlanStillRejectsResubmissionWhenPlanIsNotRejected() {
        enrollment.setStatus(StudentOfferStatus.PLANO_ENVIADO);
        when(offerStudentRepository.findForUpdateById(5L)).thenReturn(Optional.of(enrollment));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.submitWorkPlan(5L, "alunoreprovado", form));

        assertTrue(ex.getMessage().contains("Ação indisponível"));
        verify(storageService, never()).store(any(byte[].class), anyString(), anyString());
        verify(workPlanRepository, never()).save(any());
    }
}
