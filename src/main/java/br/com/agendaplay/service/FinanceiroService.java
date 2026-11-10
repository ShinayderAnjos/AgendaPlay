package br.com.agendaplay.service;
import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.model.*;
import br.com.agendaplay.repository.FinanceiroRepository;
import br.com.agendaplay.security.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class FinanceiroService {
    private static final Set<String> FORMAS=Set.of("PIX","DINHEIRO","CARTAO","TRANSFERENCIA","OUTRO");
    private final FinanceiroRepository repositorio;
    private final CaixaService caixas;
    public FinanceiroService(FinanceiroRepository repositorio,CaixaService caixas) {
        this.repositorio=repositorio; this.caixas=caixas;
    }
    public Resumo consultar(UsuarioAutenticado u,YearMonth mes) {
        validarPerfil(u,Perfil.PROPRIETARIO); return resumo(u,mes,false);
    }
    public Resumo consultarCliente(UsuarioAutenticado u,YearMonth mes) {
        validarPerfil(u,Perfil.CLIENTE); return resumo(u,mes,true);
    }
    private Resumo resumo(UsuarioAutenticado u,YearMonth mes,boolean cliente) {
        var itens=repositorio.listar(u.getId(),cliente,mes==null?null:mes.atDay(1),
            mes==null?null:mes.plusMonths(1).atDay(1));
        BigDecimal pendente=BigDecimal.ZERO,recebido=BigDecimal.ZERO,estorno=BigDecimal.ZERO;
        for(var item:itens) {
            pendente=pendente.add(item.saldoRestante());
            recebido=recebido.add(item.valorRecebido().subtract(item.valorEstornado()));
            estorno=estorno.add(item.estornoPendente());
        }
        var ids=itens.stream().map(LancamentoFinanceiro::idReserva).collect(Collectors.toSet());
        var parcelas=repositorio.parcelas(u.getId(),cliente).stream().filter(p->ids.contains(p.idReserva()))
            .collect(Collectors.groupingBy(ParcelaFinanceira::idReserva));
        return new Resumo(itens,pendente,recebido,estorno,parcelas);
    }
    @Transactional
    public void registrar(long id,String forma,UsuarioAutenticado u) {
        registrar(id,forma,null,UUID.randomUUID(),u);
    }
    @Transactional
    public void registrar(long id,String forma,BigDecimal valor,UUID chave,UsuarioAutenticado u) {
        var leitura=propria(id,u,false);
        if(forma==null || !FORMAS.contains(forma))
            throw new RegraNegocioException("Selecione uma forma de recebimento válida.");
        if(chave==null) throw new RegraNegocioException("Recarregue a página antes de registrar.");
        if(reenvio(id,forma,valor,chave)) return;
        long caixa=caixas.bloquearAberto(leitura.idEstabelecimento(),u);
        var estado=propria(id,u,true);
        if(estado.idEstabelecimento()!=leitura.idEstabelecimento())
            throw new RegraNegocioException("O estabelecimento da quadra foi alterado. Recarregue a página.");
        if(reenvio(id,forma,valor,chave)) return;
        if(estado.situacao().equals("CANCELADA"))
            throw new RegraNegocioException("Não é possível receber uma reserva cancelada.");
        var restante=estado.valorTotal().subtract(estado.recebido());
        if(restante.signum()<=0) throw new RegraNegocioException("Esta reserva já está quitada.");
        if(valor==null) valor=restante;
        CaixaService.validarValor(valor,false);
        if(valor.compareTo(restante)>0)
            throw new RegraNegocioException("O valor não pode superar o saldo restante da reserva.");
        repositorio.registrar(id,forma,valor,caixa,chave);
    }
    private boolean reenvio(long id,String forma,BigDecimal valor,UUID chave) {
        var anterior=repositorio.pelaChave(chave);
        if(anterior.isEmpty()) return false;
        var p=anterior.get();
        if(p.idReserva()==id && p.forma().equals(forma) && (valor==null || p.valor().compareTo(valor)==0))
            return true;
        throw new RegraNegocioException("Operação já utilizada. Recarregue a página.");
    }
    @Transactional
    public void registrarEstorno(long id,UsuarioAutenticado u) {
        var leitura=propria(id,u,false);
        long caixa=caixas.bloquearAberto(leitura.idEstabelecimento(),u);
        var estado=propria(id,u,true);
        if(estado.idEstabelecimento()!=leitura.idEstabelecimento())
            throw new RegraNegocioException("O estabelecimento da quadra foi alterado. Recarregue a página.");
        if(!estado.situacao().equals("CANCELADA") || estado.recebido().subtract(estado.estornado()).signum()<=0)
            throw new RegraNegocioException("Esta reserva não possui estorno pendente.");
        repositorio.estornar(id,caixa);
    }
    private FinanceiroRepository.EstadoReserva propria(long id,UsuarioAutenticado u,boolean bloquear) {
        validarPerfil(u,Perfil.PROPRIETARIO);
        var estado=repositorio.estado(id,bloquear)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(estado.idProprietario()!=u.getId())
            throw new AccessDeniedException("Esta reserva não pertence à sua conta.");
        return estado;
    }
    private void validarPerfil(UsuarioAutenticado u,Perfil perfil) {
        if(u==null || u.getPerfil()!=perfil) throw new AccessDeniedException("Acesso não permitido.");
    }
    public record Resumo(List<LancamentoFinanceiro> lancamentos,BigDecimal pendente,
        BigDecimal recebido,BigDecimal estornoPendente,Map<Long,List<ParcelaFinanceira>> parcelas) {}
}
