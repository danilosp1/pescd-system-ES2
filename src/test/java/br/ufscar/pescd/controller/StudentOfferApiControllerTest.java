package br.ufscar.pescd.controller;

import br.ufscar.pescd.dto.*;
import br.ufscar.pescd.entity.*;
import br.ufscar.pescd.service.StudentOfferService;
import br.ufscar.pescd.storage.SeaweedFsStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentOfferApiControllerTest {
    private final StudentOfferService service = mock(StudentOfferService.class);
    private final SeaweedFsStorageService storage = mock(SeaweedFsStorageService.class);
    private final StudentOfferApiController controller = new StudentOfferApiController(service, storage);
    private final Authentication auth = mock(Authentication.class);

    @Test
    void returnsListsDetailsAndSubmissionResultsForAuthenticatedStudent() {
        when(auth.getName()).thenReturn("lucas");
        StudentOfferSummaryResponse summary = mock(StudentOfferSummaryResponse.class);
        StudentEnrollmentDetailsResponse details = mock(StudentEnrollmentDetailsResponse.class);
        UserSummaryResponse professor = mock(UserSummaryResponse.class);
        when(service.listEnrollmentsForApi("lucas")).thenReturn(List.of(summary));
        when(service.getEnrollmentDetailsForApi(1L, "lucas")).thenReturn(details);
        when(service.listProfessorsForApi()).thenReturn(List.of(professor));
        assertEquals(List.of(summary), controller.listEnrollments(auth));
        assertSame(details, controller.enrollmentDetails(1L, auth));
        assertEquals(List.of(professor), controller.listProfessors());
        WorkPlanForm plan = new WorkPlanForm();
        DocumentationForm document = new DocumentationForm();
        ReportForm report = new ReportForm();
        when(service.submitWorkPlanForApi(1L, "lucas", plan)).thenReturn(details);
        when(service.submitDocumentationForApi(1L, "lucas", document)).thenReturn(details);
        when(service.submitReportForApi(1L, "lucas", report)).thenReturn(details);
        assertSame(details, controller.submitWorkPlan(1L, plan, auth));
        assertSame(details, controller.submitDocumentation(1L, document, auth));
        assertSame(details, controller.submitReport(1L, report, auth));
    }

    @Test
    void returns404ForMissingFilesWithoutReadingStorage() {
        when(auth.getName()).thenReturn("lucas");
        OfferStudent enrollment = new OfferStudent();
        when(service.getEnrollment(1L, "lucas")).thenReturn(enrollment);
        assertEquals(HttpStatus.NOT_FOUND, controller.downloadWorkPlan(1L, auth).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.downloadDocumentation(1L, auth).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.downloadReport(1L, auth).getStatusCode());
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @ValueSource(strings = "documento.pdf")
    void downloadsPdfBytesWithInlineDisposition(String name) {
        when(auth.getName()).thenReturn("lucas");
        OfferStudent enrollment = new OfferStudent();
        when(service.getEnrollment(1L, "lucas")).thenReturn(enrollment);
        byte[] bytes = {1,2,3};
        when(storage.read("fid")).thenReturn(bytes);
        WorkPlan plan = new WorkPlan();
        plan.setFileName(name);
        plan.setFileFid("fid");
        Documentation document = new Documentation();
        document.setFileName(name);
        document.setFileFid("fid");
        Report report = new Report();
        report.setFileName(name);
        report.setFileFid("fid");
        when(service.getWorkPlan(enrollment)).thenReturn(plan);
        when(service.getDocumentation(enrollment)).thenReturn(document);
        when(service.getReport(enrollment)).thenReturn(report);
        for (ResponseEntity<byte[]> response : List.of(controller.downloadWorkPlan(1L, auth),
                controller.downloadDocumentation(1L, auth), controller.downloadReport(1L, auth))) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
            assertEquals("inline; filename=\"" + name + "\"",
                    response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
            assertArrayEquals(bytes, response.getBody());
        }
    }

    @Test
    void propagatesOwnershipErrorBeforeAccessingFileStorage() {
        when(auth.getName()).thenReturn("outro");
        when(service.getEnrollment(1L, "outro")).thenThrow(new IllegalArgumentException("Sem acesso"));
        assertThrows(IllegalArgumentException.class, () -> controller.downloadWorkPlan(1L, auth));
        verifyNoInteractions(storage);
    }
}
