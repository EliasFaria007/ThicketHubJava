package thickethub.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.model.SlaConfig;
import thickethub.dto.request.SlaConfigRequest;
import thickethub.dto.response.SlaConfigResponse;
import thickethub.exception.ResourceNotFoundException;
import thickethub.repository.SlaConfigRepository;
import thickethub.service.SlaService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaServiceImpl implements SlaService {

    private final SlaConfigRepository slaConfigRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SlaConfigResponse> listarTodos() {
        return slaConfigRepository.findAll().stream()
                .map(SlaConfigResponse::de)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SlaConfigResponse buscarPorPrioridade(PrioridadeChamado prioridade) {
        SlaConfig sla = slaConfigRepository.findByPrioridade(prioridade)
                .orElseThrow(() -> new ResourceNotFoundException("Configuração de SLA não encontrada para prioridade: " + prioridade));
        return SlaConfigResponse.de(sla);
    }

    @Override
    @Transactional
    public SlaConfigResponse salvarOuAtualizar(SlaConfigRequest request) {
        SlaConfig sla = slaConfigRepository.findByPrioridade(request.prioridade())
                .orElse(SlaConfig.builder().prioridade(request.prioridade()).build());

        sla.setSlaAtendimentoHoras(request.slaAtendimentoHoras());
        sla.setSlaResolucaoHoras(request.slaResolucaoHoras());

        slaConfigRepository.save(sla);
        log.info("Configuração de SLA atualizada para prioridade: {}", request.prioridade());
        return SlaConfigResponse.de(sla);
    }
}
