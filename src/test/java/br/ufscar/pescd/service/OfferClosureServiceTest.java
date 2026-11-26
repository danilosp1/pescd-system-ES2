package br.ufscar.pescd.service;

import br.ufscar.pescd.dto.SecretaryCloseOfferPreviewResponse;
import br.ufscar.pescd.entity.Offer;
import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.enums.OfferStatus;
import br.ufscar.pescd.enums.UserRole;
import br.ufscar.pescd.repository.OfferRepository;
import br.ufscar.pescd.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OfferClosureServiceTest {

    private OfferRepository offerRepository;
    private UserRepository userRepository;
    private OfferApiMapper offerApiMapper;
    private OfferClosureService service;

    @BeforeEach
    public void setUp() {
        offerRepository = mock(OfferRepository.class);
        userRepository = mock(UserRepository.class);
        offerApiMapper = mock(OfferApiMapper.class);
        service = new OfferClosureService(offerRepository, userRepository, offerApiMapper);
    }

    private Offer createOffer(Long id, OfferStatus status) {
        Offer offer = new Offer();
        offer.setId(id);
        offer.setName("Oferta");
        offer.setStatus(status);
        return offer;
    }

    private User createSecretary() {
        User user = new User();
        user.setId(20L);
        user.setUsername("secretario");
        user.setRole(UserRole.SECRETARIO);
        return user;
    }

    @Test
    public void getCloseOfferPreviewReportsCanCloseForWaitingOffer() {
        Offer offer = createOffer(1L, OfferStatus.AGUARDANDO_ENCERRAMENTO_SECRETARIO);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(offerApiMapper.toOfferSummary(offer)).thenReturn(null);

        SecretaryCloseOfferPreviewResponse preview = service.getCloseOfferPreviewForApi(1L);

        assertTrue(preview.canClose());
        assertTrue(preview.instructions().contains("Confira os dados da oferta"));
    }

    @Test
    public void getCloseOfferPreviewReportsCannotCloseForInProgressOffer() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(offerApiMapper.toOfferSummary(offer)).thenReturn(null);

        assertFalse(service.getCloseOfferPreviewForApi(1L).canClose());
    }

    @Test
    public void getCloseOfferPreviewThrowsWhenOfferDoesNotExist() {
        when(offerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.getCloseOfferPreviewForApi(1L));
    }

    @Test
    public void closeOfferRejectsWhenNotAwaitingSecretary() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThrows(IllegalArgumentException.class, () -> service.closeOffer(1L));
        verify(offerRepository, never()).save(any());
    }

    @Test
    public void closeOfferWithoutUsernameClosesOffer() {
        Offer offer = createOffer(1L, OfferStatus.AGUARDANDO_ENCERRAMENTO_SECRETARIO);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(offerRepository.save(offer)).thenReturn(offer);

        service.closeOffer(1L);

        assertEquals(OfferStatus.CONCLUIDA, offer.getStatus());
        assertNotNull(offer.getClosedAt());
        assertNull(offer.getClosedBy());
    }

    @Test
    public void closeOfferWithUsernameRegistersSecretary() {
        Offer offer = createOffer(1L, OfferStatus.AGUARDANDO_ENCERRAMENTO_SECRETARIO);
        User secretary = createSecretary();
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userRepository.findByUsername("secretario")).thenReturn(Optional.of(secretary));
        when(offerRepository.save(offer)).thenReturn(offer);

        Offer saved = service.closeOffer(1L, "secretario");

        assertSame(secretary, saved.getClosedBy());
    }

    @Test
    public void closeOfferWithMissingSecretaryThrows() {
        Offer offer = createOffer(1L, OfferStatus.AGUARDANDO_ENCERRAMENTO_SECRETARIO);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userRepository.findByUsername("ausente")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.closeOffer(1L, "ausente"));
        verify(offerRepository, never()).save(any());
    }

    @Test
    public void closeOfferForApiMapsClosedOffer() {
        Offer offer = createOffer(1L, OfferStatus.AGUARDANDO_ENCERRAMENTO_SECRETARIO);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(offerRepository.save(offer)).thenReturn(offer);
        when(offerApiMapper.toOfferSummary(offer)).thenReturn(null);

        assertNull(service.closeOfferForApi(1L, null));
        verify(offerApiMapper).toOfferSummary(offer);
    }
}
