package br.ufscar.pescd.controller;

import br.ufscar.pescd.dto.ImportStudentsResponse;
import br.ufscar.pescd.dto.OfferForm;
import br.ufscar.pescd.dto.SecretaryCloseOfferPreviewResponse;
import br.ufscar.pescd.dto.SecretaryCloseOfferRequest;
import br.ufscar.pescd.dto.StudentForm;
import br.ufscar.pescd.service.SecretaryOfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SecretaryOfferApiControllerTest {

    private SecretaryOfferService service;
    private SecretaryOfferApiController controller;
    private Authentication authentication;

    @BeforeEach
    public void setUp() {
        service = mock(SecretaryOfferService.class);
        controller = new SecretaryOfferApiController(service);
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("secretario");
    }

    @Test
    public void listOffersDelegatesToService() {
        when(service.listOffersForApi()).thenReturn(List.of());

        assertTrue(controller.listOffers().isEmpty());
        verify(service).listOffersForApi();
    }

    @Test
    public void listProfessorsDelegatesToService() {
        when(service.listProfessorsForApi()).thenReturn(List.of());

        assertTrue(controller.listProfessors().isEmpty());
        verify(service).listProfessorsForApi();
    }

    @Test
    public void createOfferDelegatesWithAuthenticatedUsername() {
        OfferForm form = new OfferForm();

        assertNull(controller.createOffer(form, authentication));
        verify(service).createOfferForApi(form, "secretario");
    }

    @Test
    public void offerDetailsDelegatesToService() {
        assertNull(controller.offerDetails(1L));
        verify(service).getOfferDetailsForApi(1L);
    }

    @Test
    public void addStudentDelegatesToService() {
        StudentForm form = new StudentForm();

        assertNull(controller.addStudent(1L, form));
        verify(service).addStudentForApi(1L, form);
    }

    @Test
    public void importStudentsReturnsServiceResult() {
        MultipartFile file = mock(MultipartFile.class);
        ImportStudentsResponse expected = new ImportStudentsResponse(2, null);
        when(service.importStudentsFromCsvForApi(1L, file)).thenReturn(expected);

        assertSame(expected, controller.importStudents(1L, file));
    }

    @Test
    public void studentDetailsDelegatesToService() {
        assertNull(controller.studentDetails(1L, 5L));
        verify(service).getStudentDetailsForApi(1L, 5L);
    }

    @Test
    public void closePreviewDelegatesToService() {
        SecretaryCloseOfferPreviewResponse expected =
                new SecretaryCloseOfferPreviewResponse(null, true, "Instruções");
        when(service.getCloseOfferPreviewForApi(1L)).thenReturn(expected);

        assertSame(expected, controller.closePreview(1L));
    }

    @Test
    public void closeOfferDelegatesWithAuthenticatedUsername() {
        SecretaryCloseOfferRequest request = new SecretaryCloseOfferRequest(true);

        assertNull(controller.closeOffer(1L, request, authentication));
        verify(service).closeOfferForApi(1L, "secretario");
    }
}
