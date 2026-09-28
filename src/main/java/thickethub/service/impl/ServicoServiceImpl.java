package thickethub.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thickethub.domain.model.Servico;
import thickethub.domain.model.SlaConfig;
import thickethub.dto.request.ServicoRequest;
import thickethub.dto.response.ServicoResponse;
import thickethub.exception.ConflictException;
import thickethub.exception.ResourceNotFoundException;
import thickethub.repository.ServicoRepository;
import thickethub.repository.SlaConfigRepository;
import thickethub.service.ServicoService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicoServiceImpl implements ServicoService {

    private final ServicoRepository servicoRepository;
    private final SlaConfigRepository slaConfigRepository;

    @Override
    @Transactional
    public ServicoResponse criar(ServicoRequest request) {
        if (servicoRepository.existsByNomeAndSetor(request.nome(), request.setor())) {
            throw new ConflictException("Já existe um serviço com este nome no setor: " + request.setor());
        }

        SlaConfig sla = slaConfigRepository.findByPrioridade(request.prioridade()).orElse(null);

        int slaAtend = request.slaAtendimentoHoras() != null ? request.slaAtendimentoHoras()
                : (sla != null ? sla.getSlaAtendimentoHoras() : 4);
        int slaResol = request.slaResolucaoHoras() != null ? request.slaResolucaoHoras()
                : (sla != null ? sla.getSlaResolucaoHoras() : 24);

        Servico servico = Servico.builder()
                .nome(request.nome())
                .setor(request.setor())
                .prioridade(request.prioridade())
                .slaAtendimentoHoras(slaAtend)
                .slaResolucaoHoras(slaResol)
                .ativo(request.ativo() != null ? request.ativo() : true)
                .slaConfig(sla)
                .build();

        servicoRepository.save(servico);
        log.info("Serviço criado id={}, nome={}", servico.getId(), servico.getNome());
        return ServicoResponse.de(servico);
    }

    @Override
    @Transactional(readOnly = true)
    public ServicoResponse buscarPorId(Long id) {
        Servico s = servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));
        return ServicoResponse.de(s);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicoResponse> listarTodos(Boolean apenasAtivos) {
        List<Servico> lista = Boolean.TRUE.equals(apenasAtivos)
                ? servicoRepository.findByAtivoTrue()
                : servicoRepository.findAll();
        return lista.stream().map(ServicoResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicoResponse> listarPorSetor(String setor) {
        return servicoRepository.findBySetorAndAtivoTrue(setor).stream()
                .map(ServicoResponse::de)
                .toList();
    }

    @Override
    @Transactional
    public ServicoResponse atualizar(Long id, ServicoRequest request) {
        Servico s = servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));

        s.setNome(request.nome());
        s.setSetor(request.setor());
        s.setPrioridade(request.prioridade());
        if (request.slaAtendimentoHoras() != null) s.setSlaAtendimentoHoras(request.slaAtendimentoHoras());
        if (request.slaResolucaoHoras() != null) s.setSlaResolucaoHoras(request.slaResolucaoHoras());
        if (request.ativo() != null) s.setAtivo(request.ativo());

        servicoRepository.save(s);
        return ServicoResponse.de(s);
    }

    @Override
    @Transactional
    public void desativar(Long id) {
        Servico s = servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));
        s.setAtivo(false);
        servicoRepository.save(s);
    }

    @Override
    @Transactional
    public void ativar(Long id) {
        Servico s = servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));
        s.setAtivo(true);
        servicoRepository.save(s);
    }
}
