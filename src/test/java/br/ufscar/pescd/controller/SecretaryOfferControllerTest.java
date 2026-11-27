package br.ufscar.pescd.controller;

import br.ufscar.pescd.dto.OfferForm;
import br.ufscar.pescd.dto.StudentForm;
import br.ufscar.pescd.entity.Offer;
import br.ufscar.pescd.entity.OfferStudent;
import br.ufscar.pescd.entity.StatusChangeLog;
import br.ufscar.pescd.service.SecretaryOfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SecretaryOfferControllerTest {

    private SecretaryOfferService service;
    private SecretaryOfferController controller;
    private Authentication authentication;

    @BeforeEach
    public void setUp() {
        service = mock(SecretaryOfferService.class);
        controller = new SecretaryOfferController(service);
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("secretario");
    }

    private OfferForm validForm() {
        OfferForm form = new OfferForm();
        form.setName("Oferta");
        form.setSemester("2026/1");
        form.setStartDate(LocalDate.of(2026, 3, 1));
        form.setEndDate(LocalDate.of(2026, 7, 1));
        form.setResponsibleProfessorId(10L);
        return form;
    }

    private StudentForm validStudentForm() {
        StudentForm form = new StudentForm();
        form.setFullName("Aluno");
        form.setEmail("aluno@ufscar.br");
        form.setRa("123456");
        return form;
    }

    @Test
    public void listOffersAddsOffersToModel() {
        List<Offer> offers = List.of(new Offer());
        when(service.listOffers()).thenReturn(offers);
        Model model = new ExtendedModelMap();

        assertEquals("secretary/offers/list", controller.listOffers(model));
        assertSame(offers, model.getAttribute("offers"));
    }

    @Test
    public void createFormAddsEmptyFormAndProfessors() {
        when(service.listProfessors()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        assertEquals("secretary/offers/form", controller.createForm(model));
        assertTrue(model.getAttribute("offerForm") instanceof OfferForm);
        assertNotNull(model.getAttribute("professors"));
    }

    @Test
    public void createRedirectsWhenFormIsValid() {
        OfferForm form = validForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "offerForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirects = new RedirectAttributesModelMap();

        assertEquals("redirect:/secretary/offers",
                controller.create(form, bindingResult, authentication, model, redirects));
        verify(service).createOffer(form, "secretario");
        assertEquals("Oferta criada com sucesso.", redirects.getFlashAttributes().get("successMessage"));
    }

    @Test
    public void createRejectsEndDateBeforeStartDate() {
        OfferForm form = validForm();
        form.setEndDate(LocalDate.of(2026, 1, 1));
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "offerForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirects = new RedirectAttributesModelMap();
        when(service.listProfessors()).thenReturn(List.of());

        assertEquals("secretary/offers/form",
                controller.create(form, bindingResult, authentication, model, redirects));
        assertNotNull(bindingResult.getFieldError("endDate"));
        verify(service, never()).createOffer(any(), anyString());
    }

    @Test
    public void createReturnsFormWhenBeanValidationFails() {
        OfferForm form = validForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "offerForm");
        bindingResult.rejectValue("name", "required", "obrigatório");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirects = new RedirectAttributesModelMap();
        when(service.listProfessors()).thenReturn(List.of());

        assertEquals("secretary/offers/form",
                controller.create(form, bindingResult, authentication, model, redirects));
        verify(service, never()).createOffer(any(), anyString());
    }

    @Test
    public void offerDetailsAddsOfferAndStudents() {
        Offer offer = new Offer();
        List<OfferStudent> students = List.of(new OfferStudent());
        when(service.getOffer(1L)).thenReturn(offer);
        when(service.getOfferStudents(1L)).thenReturn(students);
        Model model = new ExtendedModelMap();

        assertEquals("secretary/offers/details", controller.offerDetails(1L, model));
        assertSame(offer, model.getAttribute("offer"));
        assertSame(students, model.getAttribute("students"));
    }

    @Test
    public void manageStudentsAddsEmptyFormWhenMissing() {
        when(service.getOffer(1L)).thenReturn(new Offer());
        when(service.getOfferStudents(1L)).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        assertEquals("secretary/offers/students", controller.manageStudents(1L, model));
        assertTrue(model.getAttribute("studentForm") instanceof StudentForm);
    }

    @Test
    public void addStudentRedirectsWithSuccessMessage() {
        StudentForm form = validStudentForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "studentForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirects = new RedirectAttributesModelMap();

        assertEquals("redirect:/secretary/offers/1/students",
                controller.addStudent(1L, form, bindingResult, model, redirects));
        verify(service).addStudent(1L, form);
        assertEquals("Aluno adicionado à oferta.", redirects.getFlashAttributes().get("successMessage"));
    }

    @Test
    public void addStudentReturnsFormWhenValidationFails() {
        StudentForm form = validStudentForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "studentForm");
        bindingResult.rejectValue("email", "invalid", "inválido");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirects = new RedirectAttributesModelMap();
        when(service.getOffer(1L)).thenReturn(new Offer());
        when(service.getOfferStudents(1L)).thenReturn(List.of());

        assertEquals("secretary/offers/students",
                controller.addStudent(1L, form, bindingResult, model, redirects));
        verify(service, never()).addStudent(any(), any());
    }

    @Test
    public void addStudentRedirectsWithErrorMessageOnServiceFailure() {
        StudentForm form = validStudentForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "studentForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirects = new RedirectAttributesModelMap();
        doThrow(new IllegalArgumentException("Erro de aluno"))
                .when(service).addStudent(1L, form);

        assertEquals("redirect:/secretary/offers/1/students",
                controller.addStudent(1L, form, bindingResult, model, redirects));
        assertEquals("Erro de aluno", redirects.getFlashAttributes().get("errorMessage"));
    }

    @Test
    public void importStudentsRedirectsWithSuccessAndErrorMessages() {
        MultipartFile file = mock(MultipartFile.class);
        RedirectAttributesModelMap redirects = new RedirectAttributesModelMap();
        when(service.importStudentsFromCsv(1L, file)).thenReturn(2);

        assertEquals("redirect:/secretary/offers/1/students",
                controller.importStudents(1L, file, redirects));
        assertEquals("2 aluno(s) adicionado(s) à oferta a partir do CSV.",
                redirects.getFlashAttributes().get("successMessage"));

        RedirectAttributesModelMap errorRedirects = new RedirectAttributesModelMap();
        when(service.importStudentsFromCsv(2L, file)).thenThrow(new IllegalArgumentException("CSV inválido"));

        assertEquals("redirect:/secretary/offers/2/students",
                controller.importStudents(2L, file, errorRedirects));
        assertEquals("CSV inválido", errorRedirects.getFlashAttributes().get("errorMessage"));
    }

    @Test
    public void studentDetailsAddsStudentAndLogs() {
        OfferStudent student = new OfferStudent();
        List<StatusChangeLog> logs = List.of(new StatusChangeLog());
        when(service.getOfferStudent(5L)).thenReturn(student);
        when(service.getStudentLogs(5L)).thenReturn(logs);
        Model model = new ExtendedModelMap();

        assertEquals("secretary/offers/student-details",
                controller.studentDetails(1L, 5L, model));
        assertSame(student, model.getAttribute("offerStudent"));
        assertSame(logs, model.getAttribute("logs"));
    }

    @Test
    public void closeOfferRedirectsWithSuccessAndErrorMessages() {
        RedirectAttributesModelMap redirects = new RedirectAttributesModelMap();

        assertEquals("redirect:/secretary/offers", controller.closeOffer(1L, redirects));
        verify(service).closeOffer(1L);
        assertEquals("Oferta encerrada com sucesso.", redirects.getFlashAttributes().get("successMessage"));

        RedirectAttributesModelMap errorRedirects = new RedirectAttributesModelMap();
        doThrow(new IllegalArgumentException("Status inválido")).when(service).closeOffer(2L);

        assertEquals("redirect:/secretary/offers", controller.closeOffer(2L, errorRedirects));
        assertEquals("Status inválido", errorRedirects.getFlashAttributes().get("errorMessage"));
    }
}
