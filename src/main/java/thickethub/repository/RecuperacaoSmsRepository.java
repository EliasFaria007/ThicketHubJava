package thickethub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import thickethub.domain.model.RecuperacaoSms;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RecuperacaoSmsRepository extends JpaRepository<RecuperacaoSms, Long>, JpaSpecificationExecutor<RecuperacaoSms> {
    Optional<RecuperacaoSms> findByTicketValidacaoAndUtilizadoFalse(String ticketValidacao);
    Optional<RecuperacaoSms> findTopByTelefoneAndUtilizadoFalseOrderByExpiraEmDesc(String telefone);

    @Modifying
    @Query("DELETE FROM RecuperacaoSms r WHERE r.expiraEm < :limite OR r.utilizado = true")
    void deleteExpiradosOuUtilizados(@Param("limite") LocalDateTime limite);
}
