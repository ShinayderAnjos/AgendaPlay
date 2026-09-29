package br.com.agendaplay.service;

import br.com.agendaplay.dto.QuadraForm;
import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.security.UsuarioAutenticado;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

@Service
public class FotoService {
    private final JdbcClient jdbc;
    private final QuadraService quadras;

    public FotoService(JdbcClient jdbc, QuadraService quadras) {
        this.jdbc = jdbc;
        this.quadras = quadras;
    }

    public record Foto(long id, long idQuadra, boolean capa, String nome) {}

    public List<Foto> listar(long id) {
        return jdbc.sql(
                        "SELECT f.id,f.id_quadra,f.capa,q.nome FROM foto_quadra f JOIN quadra q ON"
                            + " q.id=f.id_quadra WHERE q.id=? ORDER BY f.capa DESC,f.id")
                .param(id)
                .query(Foto.class)
                .list();
    }

    public List<Foto> destaques() {
        return jdbc.sql(
                        "SELECT f.id,f.id_quadra,f.capa,q.nome FROM foto_quadra f JOIN quadra q ON"
                            + " q.id=f.id_quadra WHERE q.situacao='ATIVA' ORDER BY f.capa DESC,f.id"
                            + " DESC LIMIT 30")
                .query(Foto.class)
                .list();
    }

    public Map<Long, Long> capas() {
        var m = new HashMap<Long, Long>();
        jdbc.sql(
                        "SELECT f.id,f.id_quadra,f.capa,q.nome FROM foto_quadra f JOIN quadra q ON"
                            + " q.id=f.id_quadra WHERE capa")
                .query(Foto.class)
                .list()
                .forEach(f -> m.put(f.idQuadra(), f.id()));
        return m;
    }

    public byte[] conteudo(long id, UsuarioAutenticado usuario) {
        var q =
                jdbc.sql("SELECT id_quadra FROM foto_quadra WHERE id=?")
                        .param(id)
                        .query(Long.class)
                        .optional()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var quadra = quadras.buscar(q);
        if (!quadra.situacao().equals("ATIVA")
                && (usuario == null || quadra.idProprietario() != usuario.getId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return jdbc.sql("SELECT conteudo FROM foto_quadra WHERE id=?")
                .param(id)
                .query(byte[].class)
                .single();
    }

    private byte[] validar(MultipartFile arquivo) {
        if (arquivo.getSize() > 5 * 1024 * 1024)
            throw new RegraNegocioException("Cada foto deve ter no máximo 5 MB.");
        try (var input = ImageIO.createImageInputStream(arquivo.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext())
                throw new RegraNegocioException("Envie uma imagem JPEG ou PNG válida.");
            var reader = readers.next();
            try {
                reader.setInput(input);
                String formato = reader.getFormatName();
                if (!formato.equalsIgnoreCase("png") && !formato.equalsIgnoreCase("jpeg"))
                    throw new RegraNegocioException("Use fotos JPEG ou PNG.");
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 20000000L)
                    throw new RegraNegocioException("A foto deve ter até 20 megapixels.");
                var imagem = reader.read(0);
                var output = new ByteArrayOutputStream();
                ImageIO.write(imagem, "png", output);
                return output.toByteArray();
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new RegraNegocioException(
                    "Não foi possível ler a foto. Envie outro arquivo JPEG ou PNG.");
        }
    }

    @Transactional
    public long salvarQuadra(
            Long id, QuadraForm form, UsuarioAutenticado usuario, List<MultipartFile> arquivos) {
        long q = quadras.salvar(id, form, usuario);
        adicionar(q, arquivos, usuario);
        return q;
    }

    @Transactional
    public void adicionar(long id, List<MultipartFile> arquivos, UsuarioAutenticado usuario) {
        quadras.propria(id, usuario);
        jdbc.sql("SELECT id FROM quadra WHERE id=? FOR UPDATE")
                .param(id)
                .query(Long.class)
                .single();
        var novas =
                arquivos == null
                        ? List.<MultipartFile>of()
                        : arquivos.stream().filter(f -> !f.isEmpty()).toList();
        var existentes = listar(id);
        if (existentes.size() + novas.size() > 8)
            throw new RegraNegocioException("Cadastre até 8 fotos por quadra.");
        var imagens = novas.stream().map(this::validar).toList();
        boolean primeira = existentes.isEmpty();
        for (var bytes : imagens) {
            jdbc.sql("INSERT INTO foto_quadra(id_quadra,conteudo,capa) VALUES (?,?,?)")
                    .params(id, bytes, primeira)
                    .update();
            primeira = false;
        }
    }

    @Transactional
    public void alterar(long quadra, long foto, boolean excluir, UsuarioAutenticado usuario) {
        quadras.propria(quadra, usuario);
        jdbc.sql("SELECT id FROM quadra WHERE id=? FOR UPDATE")
                .param(quadra)
                .query(Long.class)
                .single();
        var alvo =
                listar(quadra).stream()
                        .filter(f -> f.id() == foto)
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (excluir) {
            jdbc.sql("DELETE FROM foto_quadra WHERE id=?").param(foto).update();
            if (alvo.capa())
                jdbc.sql(
                                "UPDATE foto_quadra SET capa=true WHERE id=(SELECT min(id) FROM"
                                    + " foto_quadra WHERE id_quadra=?)")
                        .param(quadra)
                        .update();
        } else {
            jdbc.sql("UPDATE foto_quadra SET capa=false WHERE id_quadra=?").param(quadra).update();
            jdbc.sql("UPDATE foto_quadra SET capa=true WHERE id=?").param(foto).update();
        }
    }
}
