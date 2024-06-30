package med.voll.api.domain.consulta;

import med.voll.api.domain.ValidacaoException;
import med.voll.api.domain.consulta.validacoes.agendamento.ValidadorAgendamentoDeConsulta;
import med.voll.api.domain.consulta.validacoes.cancelamento.ValidadorCancelamentoDeConsulta;
import med.voll.api.domain.medico.Medico;
import med.voll.api.domain.medico.MedicoRepository;
import med.voll.api.domain.paciente.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgendaDeConsultas {

    @Autowired
    private ConsultaRepository consultaRepository;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private List<ValidadorAgendamentoDeConsulta> validadoresAgendamento;

    @Autowired
    private List<ValidadorCancelamentoDeConsulta> validadoresCancelamento;

    public DadosDetalhamentoConsulta agendar(DadosAgendamentoConsulta dados) {
        if (!this.pacienteRepository.existsById(dados.idPaciente()))  {
            throw new ValidacaoException("Id do paciente informado não existe");
        }

        if (dados.idMedico() != null && !this.medicoRepository.existsById(dados.idMedico()))  {
            throw new ValidacaoException("Id do médico informado não existe");
        }

        // S (Princípio da Responsabilidade Única)
        // O (Princípio aberto-fechado)
        // D (Princípio da inversão de dependência)
        validadoresAgendamento.forEach(v -> v.validar(dados));

        var consulta = getConsulta(dados);
        this.consultaRepository.save(consulta);

        return new DadosDetalhamentoConsulta(consulta);
    }

    public void cancelar(DadosCancelamentoConsulta dados) {
        if (!consultaRepository.existsById(dados.idConsulta())) {
            throw new ValidacaoException("Id da consulta informado não existe!");
        }

        validadoresCancelamento.forEach(v -> v.validar(dados));

        var consulta = consultaRepository.getReferenceById(dados.idConsulta());
        consulta.cancelar(dados.motivo());
    }

    private Consulta getConsulta(DadosAgendamentoConsulta dados) {
        var medico = this.escolherMedico(dados);
        var paciente = this.pacienteRepository.getReferenceById(dados.idPaciente());
        return new Consulta(null, medico, paciente, dados.data(), null);
    }

    private Medico escolherMedico(DadosAgendamentoConsulta dados) {
        if (dados.idMedico() != null) {
            return this.medicoRepository.getReferenceById(dados.idMedico());
        }

        if (dados.especialidade() == null) {
            throw new ValidacaoException("Especialidade é obrigatória quando médico não for informado");
        }

        var medico = this.medicoRepository.escolherMedicoAleatorioLivreNaData(dados.especialidade(), dados.data());

        if (medico == null) {
            throw new ValidacaoException("Não existe médico disponível nessa data");
        }

        return medico;
    }
}
