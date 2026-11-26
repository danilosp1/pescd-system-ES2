package br.ufscar.pescd.service;

import br.ufscar.pescd.dto.OfferView;
import br.ufscar.pescd.entity.Offer;
import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.repository.OfferRepository;
import br.ufscar.pescd.repository.OfferStudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OfferServiceTest {

    private OfferRepository offerRepository;
    private OfferStudentRepository offerStudentRepository;
    private OfferService offerService;

    @BeforeEach
    public void setUp() {
        offerRepository = mock(OfferRepository.class);
        offerStudentRepository = mock(OfferStudentRepository.class);
        offerService = new OfferService(offerRepository, offerStudentRepository);
    }

    private Offer createOffer(String name, String semester, User responsible) {
        Offer offer = new Offer();
        offer.setName(name);
        offer.setSemester(semester);
        offer.setStartDate(LocalDate.of(2026, 3, 1));
        offer.setEndDate(LocalDate.of(2026, 7, 1));
        offer.setResponsibleProfessor(responsible);
        return offer;
    }

    @Test
    public void findPublicOffersMapsFieldsAndEnrollmentCount() {
        User professor = new User();
        professor.setFullName("Alexandre Levada");
        Offer offer = createOffer("Algoritmo", "2026/1", professor);

        when(offerRepository.findAllByOrderBySemesterDesc()).thenReturn(List.of(offer));
        when(offerStudentRepository.countByOffer(offer)).thenReturn(3L);

        List<OfferView> result = offerService.findPublicOffers();

        assertEquals(1, result.size());
        OfferView view = result.get(0);
        assertEquals("Algoritmo", view.name());
        assertEquals("2026/1", view.semester());
        assertEquals(LocalDate.of(2026, 3, 1), view.startDate());
        assertEquals(LocalDate.of(2026, 7, 1), view.endDate());
        assertEquals("Alexandre Levada", view.responsibleProfessor());
        assertEquals(3L, view.enrolledStudents());
    }

    @Test
    public void findPublicOffersUsesDefaultProfessorNameWhenResponsibleIsNull() {
        Offer offer = createOffer("Sem responsável", "2026/1", null);

        when(offerRepository.findAllByOrderBySemesterDesc()).thenReturn(List.of(offer));
        when(offerStudentRepository.countByOffer(offer)).thenReturn(0L);

        OfferView view = offerService.findPublicOffers().get(0);

        assertEquals("Não definido", view.responsibleProfessor());
        assertEquals(0L, view.enrolledStudents());
    }

    @Test
    public void findPublicOffersReturnsEmptyListWhenThereAreNoOffers() {
        when(offerRepository.findAllByOrderBySemesterDesc()).thenReturn(List.of());

        assertTrue(offerService.findPublicOffers().isEmpty());
    }
}
