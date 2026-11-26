package br.ufscar.pescd.service;

import br.ufscar.pescd.dto.OfferSummaryResponse;
import br.ufscar.pescd.dto.SecretaryCloseOfferPreviewResponse;
import br.ufscar.pescd.entity.Offer;
import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.enums.OfferStatus;
import br.ufscar.pescd.repository.OfferRepository;
import br.ufscar.pescd.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Concentra o encerramento de ofertas: prévia, confirmação e registro do
 * secretário responsável. Separado do SecretaryOfferService para manter
 * coesas as responsabilidades de gestão de oferta/alunos.
 */
@Service
public class OfferClosureService {

    private static final String CLOSE_OFFER_INSTRUCTIONS =
            "Confira os dados da oferta antes de confirmar o encerramento. "
                    + "Ao confirmar, os créditos serão atribuídos aos alunos e a oferta será concluída.";

    private final OfferRepository offerRepository;
    private final UserRepository userRepository;
    private final OfferApiMapper offerApiMapper;

    public OfferClosureService(
            OfferRepository offerRepository,
            UserRepository userRepository,
            OfferApiMapper offerApiMapper
    ) {
        this.offerRepository = offerRepository;
        this.userRepository = userRepository;
        this.offerApiMapper = offerApiMapper;
    }

    @Transactional(readOnly = true)
    public SecretaryCloseOfferPreviewResponse getCloseOfferPreviewForApi(Long offerId) {
        Offer offer = getOffer(offerId);
        return new SecretaryCloseOfferPreviewResponse(
                offerApiMapper.toOfferSummary(offer),
                offer.getStatus() == OfferStatus.AGUARDANDO_ENCERRAMENTO_SECRETARIO,
                CLOSE_OFFER_INSTRUCTIONS
        );
    }

    @Transactional
    public void closeOffer(Long id) {
        closeOffer(id, null);
    }

    @Transactional
    public OfferSummaryResponse closeOfferForApi(Long id, String secretaryUsername) {
        return offerApiMapper.toOfferSummary(closeOffer(id, secretaryUsername));
    }

    @Transactional
    public Offer closeOffer(Long id, String secretaryUsername) {
        Offer offer = getOffer(id);

        if (offer.getStatus() != OfferStatus.AGUARDANDO_ENCERRAMENTO_SECRETARIO) {
            throw new IllegalArgumentException(
                    "A oferta ainda não está aguardando encerramento do secretário."
            );
        }

        offer.setStatus(OfferStatus.CONCLUIDA);
        offer.setClosedAt(LocalDateTime.now());
        if (secretaryUsername != null) {
            User secretary = userRepository.findByUsername(secretaryUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Secretário autenticado não encontrado."));
            offer.setClosedBy(secretary);
        }

        return offerRepository.save(offer);
    }

    private Offer getOffer(Long id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Oferta não encontrada."));
    }
}
