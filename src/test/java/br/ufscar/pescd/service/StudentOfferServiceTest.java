package br.ufscar.pescd.service;

import br.ufscar.pescd.dto.*;
import br.ufscar.pescd.entity.*;
import br.ufscar.pescd.enums.*;
import br.ufscar.pescd.repository.*;
import br.ufscar.pescd.storage.SeaweedFsStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentOfferServiceTest {
    private final OfferStudentRepository enrollments = mock(OfferStudentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final WorkPlanRepository plans = mock(WorkPlanRepository.class);
    private final DocumentationRepository documents = mock(DocumentationRepository.class);
    private final ReportRepository reports = mock(ReportRepository.class);
    private final StatusChangeLogRepository logs = mock(StatusChangeLogRepository.class);
    private final OfferApiMapper mapper = mock(OfferApiMapper.class);
    private final SeaweedFsStorageService storage = mock(SeaweedFsStorageService.class);
    private final StudentOfferService service = new StudentOfferService(
            enrollments, users, plans, documents, reports, logs, mapper, storage);
    private User student;
    private User professor;
    private OfferStudent enrollment;
    private final byte[] pdf = "%PDF-1.4 example".getBytes();

    @BeforeEach
    void setUp() {
        student = new User();
        student.setUsername("aluno");
        professor = new User();
        professor.setId(3L);
        professor.setRole(UserRole.PROFESSOR);
        Offer offer = new Offer();
        offer.setStatus(OfferStatus.EM_ANDAMENTO);
        enrollment = new OfferStudent();
        enrollment.setId(1L);
        enrollment.setStudent(student);
        enrollment.setOffer(offer);
        enrollment.setStatus(StudentOfferStatus.NAO_ENVIADO);
    }

    private void enrolled() {
        when(enrollments.findById(1L)).thenReturn(Optional.of(enrollment));
    }

    private void enrolledForUpdate() {
        when(enrollments.findForUpdateById(1L)).thenReturn(Optional.of(enrollment));
    }

    private WorkPlanForm planForm(MultipartFile file) {
        WorkPlanForm form = new WorkPlanForm();
        form.setSupervisorId(3L);
        form.setDisciplineCode("ES01");
        form.setDisciplineName("Engenharia de Software");
        form.setDisciplineCourse("Computação");
        form.setFile(file);
        return form;
    }

    private MultipartFile pdfFile() {
        return new MockMultipartFile("file", "plano.pdf", "application/pdf", pdf);
    }

    private void assertTransition(StudentOfferStatus previous, StudentOfferStatus next) {
        ArgumentCaptor<StatusChangeLog> captor = ArgumentCaptor.forClass(StatusChangeLog.class);
        verify(logs).save(captor.capture());
        StatusChangeLog log = captor.getValue();
        assertSame(enrollment, log.getOfferStudent());
        assertEquals(previous, log.getPreviousStatus());
        assertEquals(next, log.getNewStatus());
        assertFalse(log.getDescription().isBlank());
        assertEquals(next, enrollment.getStatus());
        verify(enrollments).save(enrollment);
    }

    @Test
    void listsEnrollmentsAndProfessorsThroughApiMapper() {
        when(users.findByUsername("aluno")).thenReturn(Optional.of(student));
        when(enrollments.findByStudent(student)).thenReturn(List.of(enrollment));
        when(users.findByRole(UserRole.PROFESSOR)).thenReturn(List.of(professor));
        StudentOfferSummaryResponse summary = mock(StudentOfferSummaryResponse.class);
        UserSummaryResponse user = mock(UserSummaryResponse.class);
        when(mapper.toStudentOfferSummary(enrollment)).thenReturn(summary);
        when(mapper.toUserSummary(professor)).thenReturn(user);
        assertEquals(List.of(summary), service.listEnrollmentsForApi("aluno"));
        assertEquals(List.of(user), service.listProfessorsForApi());
        assertThrows(IllegalArgumentException.class, () -> service.findStudentEnrollments("ausente"));
    }

    @Test
    void checksOwnershipAndMissingEnrollmentBeforeReturningDetails() {
        assertThrows(IllegalArgumentException.class, () -> service.getEnrollment(1L, "aluno"));
        enrolled();
        assertThrows(IllegalArgumentException.class, () -> service.getEnrollment(1L, "outro"));
        StudentEnrollmentDetailsResponse details = mock(StudentEnrollmentDetailsResponse.class);
        when(mapper.toStudentEnrollmentDetails(enrollment)).thenReturn(details);
        assertSame(details, service.getEnrollmentDetailsForApi(1L, "aluno"));
    }

    @Test
    void returnsOptionalFilesAndStatusHistory() {
        assertNull(service.getWorkPlan(enrollment));
        assertNull(service.getDocumentation(enrollment));
        assertNull(service.getReport(enrollment));
        WorkPlan plan = new WorkPlan();
        Documentation document = new Documentation();
        Report report = new Report();
        when(plans.findByOfferStudent(enrollment)).thenReturn(Optional.of(plan));
        when(documents.findByOfferStudent(enrollment)).thenReturn(Optional.of(document));
        when(reports.findByOfferStudent(enrollment)).thenReturn(Optional.of(report));
        StatusChangeLog log = new StatusChangeLog();
        when(logs.findByOfferStudentOrderByChangedAtAsc(enrollment)).thenReturn(List.of(log));
        assertSame(plan, service.getWorkPlan(enrollment));
        assertSame(document, service.getDocumentation(enrollment));
        assertSame(report, service.getReport(enrollment));
        assertEquals(List.of(log), service.getStatusLogs(enrollment));
    }

    @Test
    void submitsPlanAndPersistsMetadataSupervisorAndHistory() {
        enrolled();
        when(users.findById(3L)).thenReturn(Optional.of(professor));
        when(storage.store(pdf, "plano.pdf", "application/pdf")).thenReturn("new-fid");
        StudentEnrollmentDetailsResponse details = mock(StudentEnrollmentDetailsResponse.class);
        when(mapper.toStudentEnrollmentDetails(enrollment)).thenReturn(details);
        assertSame(details, service.submitWorkPlanForApi(1L, "aluno", planForm(pdfFile())));
        ArgumentCaptor<WorkPlan> captor = ArgumentCaptor.forClass(WorkPlan.class);
        verify(plans).save(captor.capture());
        WorkPlan plan = captor.getValue();
        assertSame(enrollment, plan.getOfferStudent());
        assertEquals("ES01", plan.getDisciplineCode());
        assertEquals("Engenharia de Software", plan.getDisciplineName());
        assertEquals("Computação", plan.getDisciplineCourse());
        assertEquals("plano.pdf", plan.getFileName());
        assertEquals("application/pdf", plan.getContentType());
        assertEquals("new-fid", plan.getFileFid());
        assertSame(professor, enrollment.getSupervisor());
        assertTransition(StudentOfferStatus.NAO_ENVIADO, StudentOfferStatus.PLANO_ENVIADO);
    }

    @Test
    void refusesWorkPlanWhenSelectedSupervisorIsAStudent() {
        enrolled();
        User candidate = new User();
        candidate.setId(3L);
        candidate.setRole(UserRole.ALUNO);
        when(users.findById(3L)).thenReturn(Optional.of(candidate));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.submitWorkPlan(1L, "aluno", planForm(pdfFile())));

        assertEquals("Professor supervisor inválido.", error.getMessage());
        assertEquals(StudentOfferStatus.NAO_ENVIADO, enrollment.getStatus());
        assertNull(enrollment.getSupervisor());
        verify(enrollments, never()).save(any());
        verifyNoInteractions(storage, plans, logs);
    }

    @Test
    void reusesExistingPlanAndRemovesItsPreviousFile() {
        enrolled();
        when(users.findById(3L)).thenReturn(Optional.of(professor));
        WorkPlan old = new WorkPlan();
        old.setFileFid("old");
        when(plans.findByOfferStudent(enrollment)).thenReturn(Optional.of(old));
        service.submitWorkPlan(1L, "aluno", planForm(pdfFile()));
        verify(storage).delete("old");
        verify(plans).save(old);
    }


    @ParameterizedTest
    @EnumSource(value = OfferStatus.class, names = "EM_ANDAMENTO", mode = EnumSource.Mode.EXCLUDE)
    void rejectsAllUploadsOnOffersOutsideInProgress(OfferStatus status) {
        enrolled();
        enrolledForUpdate();
        enrollment.getOffer().setStatus(status);
        assertThrows(IllegalArgumentException.class, () -> service.submitWorkPlan(1L, "aluno", planForm(pdfFile())));
        assertThrows(IllegalArgumentException.class, () -> service.submitDocumentation(1L, "aluno", new DocumentationForm()));
        assertThrows(IllegalArgumentException.class, () -> service.submitReport(1L, "aluno", new ReportForm()));
        verifyNoInteractions(storage, plans, documents, reports, logs);
    }

    @ParameterizedTest
    @EnumSource(StudentOfferStatus.class)
    void enforcesTheRequiredEnrollmentStatusForEveryUpload(StudentOfferStatus status) {
        enrolled();
        enrollment.setStatus(status);
        if (status != StudentOfferStatus.NAO_ENVIADO) {
            assertThrows(IllegalArgumentException.class, () -> service.submitWorkPlan(1L, "aluno", planForm(pdfFile())));
            assertThrows(IllegalArgumentException.class, () -> service.submitDocumentation(1L, "aluno", new DocumentationForm()));
        }
        if (status != StudentOfferStatus.PLANO_APROVADO && status != StudentOfferStatus.RELATORIO_REPROVADO) {
            enrolledForUpdate();
            assertThrows(IllegalArgumentException.class, () -> service.submitReport(1L, "aluno", new ReportForm()));
        }
        verifyNoInteractions(storage, logs);
    }

    static Stream<MultipartFile> invalidFiles() throws IOException {
        MultipartFile unreadable = mock(MultipartFile.class);
        when(unreadable.getContentType()).thenReturn("application/pdf");
        when(unreadable.getBytes()).thenThrow(new IOException("read failure"));
        MultipartFile unnamed = mock(MultipartFile.class);
        when(unnamed.getContentType()).thenReturn("text/plain");
        return Stream.of(null,
                new MockMultipartFile("file", new byte[0]), unreadable, unnamed,
                new MockMultipartFile("file", "texto.txt", "text/plain", new byte[]{1}),
                new MockMultipartFile("file", null, null, new byte[]{1}));
    }

    @ParameterizedTest
    @MethodSource("invalidFiles")
    void rejectsInvalidPdfsWithoutPersistingOrTouchingStorage(MultipartFile file) {
        enrolled();
        DocumentationForm form = new DocumentationForm();
        form.setFile(file);
        assertThrows(IllegalArgumentException.class, () -> service.submitDocumentation(1L, "aluno", form));
        verifyNoInteractions(storage, documents, logs);
        verify(enrollments, never()).save(any());
    }

    @Test
    void rejectsDocumentationOneByteAboveTheFiveMegabyteLimit() {
        enrolled();
        DocumentationForm form = new DocumentationForm();
        form.setFile(new MockMultipartFile("file", "aulas.pdf", "application/pdf",
                new byte[5 * 1024 * 1024 + 1]));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.submitDocumentation(1L, "aluno", form));

        assertEquals("O arquivo deve ter no máximo 5 MB.", error.getMessage());
        assertEquals(StudentOfferStatus.NAO_ENVIADO, enrollment.getStatus());
        verify(enrollments, never()).save(any());
        verifyNoInteractions(storage, documents, logs);
    }


    @Test
    void submitsDocumentationAndReusesExistingEntity() {
        enrolled();
        Documentation old = new Documentation();
        old.setFileFid("old-document");
        when(documents.findByOfferStudent(enrollment)).thenReturn(Optional.of(old));
        when(storage.store(pdf, "plano.pdf", "application/pdf")).thenReturn("document-fid");
        DocumentationForm form = new DocumentationForm();
        form.setInstitutionName("UFSCar");
        form.setDisciplineName("Didática");
        form.setDisciplineCourse("Pós-graduação");
        form.setWorkloadHours(60);
        form.setFile(pdfFile());
        StudentEnrollmentDetailsResponse details = mock(StudentEnrollmentDetailsResponse.class);
        when(mapper.toStudentEnrollmentDetails(enrollment)).thenReturn(details);
        assertSame(details, service.submitDocumentationForApi(1L, "aluno", form));
        verify(documents).save(old);
        verify(storage).delete("old-document");
        assertEquals("UFSCar", old.getInstitutionName());
        assertEquals("Didática", old.getDisciplineName());
        assertEquals("Pós-graduação", old.getDisciplineCourse());
        assertEquals(60, old.getWorkloadHours());
        assertEquals("document-fid", old.getFileFid());
        assertTransition(StudentOfferStatus.NAO_ENVIADO, StudentOfferStatus.DOCUMENTACAO_ENVIADA);
    }

    @Test
    void submitsFinalReportAfterPlanApproval() {
        enrolled();
        enrolledForUpdate();
        enrollment.setStatus(StudentOfferStatus.PLANO_APROVADO);
        ReportForm form = new ReportForm();
        form.setFrequency(95);
        form.setFile(pdfFile());
        when(storage.store(pdf, "plano.pdf", "application/pdf")).thenReturn("report-fid");
        StudentEnrollmentDetailsResponse details = mock(StudentEnrollmentDetailsResponse.class);
        when(mapper.toStudentEnrollmentDetails(enrollment)).thenReturn(details);
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertSame(details, service.submitReportForApi(1L, "aluno", form));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reports).save(captor.capture());
        assertEquals(95, captor.getValue().getFrequency());
        assertEquals("report-fid", captor.getValue().getFileFid());
        assertTransition(StudentOfferStatus.PLANO_APROVADO, StudentOfferStatus.RELATORIO_ENVIADO);
    }

    @Test
    void storageFailureDoesNotSaveReportOrAdvanceStatus() {
        enrolledForUpdate();
        enrollment.setStatus(StudentOfferStatus.PLANO_APROVADO);
        Report report = new Report();
        report.setFileFid("old-report");
        when(reports.findByOfferStudent(enrollment)).thenReturn(Optional.of(report));
        when(storage.store(any(), any(), any())).thenThrow(new IllegalStateException("offline"));
        ReportForm form = new ReportForm();
        form.setFile(pdfFile());
        assertThrows(IllegalStateException.class, () -> service.submitReport(1L, "aluno", form));
        verify(storage, never()).delete(any());
        verify(reports, never()).save(any());
        verifyNoInteractions(logs);
        assertEquals("old-report", report.getFileFid());
        assertEquals(StudentOfferStatus.PLANO_APROVADO, enrollment.getStatus());
    }
}
