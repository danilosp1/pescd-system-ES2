package br.ufscar.pescd.service;

import br.ufscar.pescd.entity.Offer;
import br.ufscar.pescd.entity.OfferStudent;
import br.ufscar.pescd.entity.StatusChangeLog;
import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.entity.WorkPlan;
import br.ufscar.pescd.enums.StudentOfferStatus;
import br.ufscar.pescd.repository.OfferStudentRepository;
import br.ufscar.pescd.repository.ReportRepository;
import br.ufscar.pescd.repository.StatusChangeLogRepository;
import br.ufscar.pescd.repository.WorkPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SupervisorProfessorServiceMud2Test {

    private OfferStudentRepository offerStudentRepository;
    private WorkPlanRepository workPlanRepository;
    private ReportRepository reportRepository;
    private StatusChangeLogRepository statusChangeLogRepository;
    private OfferApiMapper offerApiMapper;
    private SupervisorProfessorService service;

    private User professor;
    private User otherProfessor;
    private OfferStudent enrollment;
    private WorkPlan workPlan;

    @BeforeEach
    public void setUp() {
        offerStudentRepository = mock(OfferStudentRepository.class);
        workPlanRepository = mock(WorkPlanRepository.class);
        reportRepository = mock(ReportRepository.class);
        statusChangeLogRepository = mock(StatusChangeLogRepository.class);
        offerApiMapper = mock(OfferApiMapper.class);
        service = new SupervisorProfessorService(
                offerStudentRepository,
                workPlanRepository,
                reportRepository,
                statusChangeLogRepository,
                offerApiMapper
        );

        professor = new User();
        professor.setId(1L);
        professor.setUsername("alevada");

        otherProfessor = new User();
        otherProfessor.setId(2L);
        otherProfessor.setUsername("aendo");

        Offer offer = new Offer();
        offer.setId(10L);

        enrollment = new OfferStudent();
        enrollment.setId(5L);
        enrollment.setOffer(offer);
        enrollment.setSupervisor(professor);
        enrollment.setStatus(StudentOfferStatus.PLANO_ENVIADO);

        workPlan = new WorkPlan();
        workPlan.setId(7L);
        workPlan.setOfferStudent(enrollment);
        workPlan.setSupervisorParecer("Parecer anterior");
        workPlan.setSupervisorApprovedAt(LocalDateTime.now().minusDays(2));
    }

    private void mockEnrollmentAndPlan() {
        when(offerStudentRepository.findByOffer_IdAndStudent_Id(10L, 5L))
                .thenReturn(Optional.of(enrollment));
        when(workPlanRepository.findByOfferStudent(enrollment))
                .thenReturn(Optional.of(workPlan));
    }

    @Test
    public void rejectPlanChangesStatusAndStoresParecer() {
        mockEnrollmentAndPlan();

        service.rejectPlan(10L, 5L, professor, "Plano precisa ser mais detalhado.");

        assertEquals(StudentOfferStatus.PLANO_REPROVADO, enrollment.getStatus());
        assertEquals("Plano precisa ser mais detalhado.", workPlan.getSupervisorParecer());
        assertNotNull(workPlan.getSupervisorRejectedAt());
        assertNull(workPlan.getSupervisorApprovedAt());
        verify(workPlanRepository).save(workPlan);
        verify(offerStudentRepository).save(enrollment);

        ArgumentCaptor<StatusChangeLog> logCaptor = ArgumentCaptor.forClass(StatusChangeLog.class);
        verify(statusChangeLogRepository).save(logCaptor.capture());
        assertEquals(StudentOfferStatus.PLANO_ENVIADO, logCaptor.getValue().getPreviousStatus());
        assertEquals(StudentOfferStatus.PLANO_REPROVADO, logCaptor.getValue().getNewStatus());
        assertEquals("Plano de trabalho reprovado pelo professor supervisor",
                logCaptor.getValue().getDescription());
    }

    @Test
    public void rejectPlanForApiDelegatesAndMapsEnrollment() {
        mockEnrollmentAndPlan();
        when(offerApiMapper.toOfferStudentSummary(enrollment)).thenReturn(null);

        assertNull(service.rejectPlanForApi(10L, 5L, professor, new br.ufscar.pescd.dto.RejectPlanFormDTO()));
        verify(offerApiMapper).toOfferStudentSummary(enrollment);
    }

    @Test
    public void rejectPlanThrowsWhenStatusIsNotPlanoEnviado() {
        enrollment.setStatus(StudentOfferStatus.PLANO_APROVADO);
        mockEnrollmentAndPlan();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.rejectPlan(10L, 5L, professor, "Parecer"));

        assertTrue(ex.getMessage().contains("Ação indisponível"));
        verify(workPlanRepository, never()).save(any());
    }

    @Test
    public void rejectPlanThrowsWhenProfessorIsNotSupervisor() {
        enrollment.setSupervisor(otherProfessor);
        mockEnrollmentAndPlan();

        assertThrows(AccessDeniedException.class, () ->
                service.rejectPlan(10L, 5L, professor, "Parecer"));
    }

    @Test
    public void rejectPlanThrowsWhenPlanDoesNotExist() {
        when(offerStudentRepository.findByOffer_IdAndStudent_Id(10L, 5L))
                .thenReturn(Optional.of(enrollment));
        when(workPlanRepository.findByOfferStudent(enrollment)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                service.rejectPlan(10L, 5L, professor, "Parecer"));
    }

    @Test
    public void approvePlanClearsPreviousRejection() {
        enrollment.setStatus(StudentOfferStatus.PLANO_ENVIADO);
        workPlan.setSupervisorRejectedAt(LocalDateTime.now().minusDays(1));
        workPlan.setSupervisorApprovedAt(null);
        mockEnrollmentAndPlan();

        service.approvePlan(10L, 5L, professor, "Plano aprovado.");

        assertEquals(StudentOfferStatus.PLANO_APROVADO, enrollment.getStatus());
        assertNotNull(workPlan.getSupervisorApprovedAt());
        assertNull(workPlan.getSupervisorRejectedAt());
        assertEquals("Plano aprovado.", workPlan.getSupervisorParecer());
    }
}
