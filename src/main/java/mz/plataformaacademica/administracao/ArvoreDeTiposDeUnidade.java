package mz.plataformaacademica.administracao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import mz.plataformaacademica.comum.RegraDeNegocioException;

public final class ArvoreDeTiposDeUnidade {

    public static final int MAXIMO_DE_TIPOS = 50;
    public static final int TAMANHO_MAXIMO_DO_NOME = 100;

    public record Entrada(String nome, String paiNome) {
    }

    public record TipoResolvido(String nome, int nivel, String paiNome) {
    }

    private ArvoreDeTiposDeUnidade() {
    }

    public static String chave(String nome) {
        return nome.strip().toLowerCase(Locale.ROOT);
    }

    public static List<TipoResolvido> resolver(List<Entrada> entradas) {
        if (entradas == null || entradas.isEmpty()) {
            throw new RegraDeNegocioException("O modelo precisa de pelo menos um tipo de unidade");
        }
        if (entradas.size() > MAXIMO_DE_TIPOS) {
            throw new RegraDeNegocioException(
                "O modelo não pode ter mais de " + MAXIMO_DE_TIPOS + " tipos de unidade");
        }
        List<String> nomes = new ArrayList<>();
        List<String> pais = new ArrayList<>();
        Set<String> chaves = new HashSet<>();
        for (Entrada entrada : entradas) {
            if (entrada == null || entrada.nome() == null || entrada.nome().isBlank()) {
                throw new RegraDeNegocioException("O nome de cada tipo de unidade é obrigatório");
            }
            String nome = entrada.nome().strip();
            if (nome.length() > TAMANHO_MAXIMO_DO_NOME) {
                throw new RegraDeNegocioException("O nome do tipo de unidade '" + nome + "' é demasiado longo");
            }
            if (!chaves.add(chave(nome))) {
                throw new RegraDeNegocioException("O tipo de unidade '" + nome + "' aparece mais de uma vez");
            }
            nomes.add(nome);
            pais.add(entrada.paiNome() == null || entrada.paiNome().isBlank() ? null : entrada.paiNome().strip());
        }
        for (int i = 0; i < nomes.size(); i++) {
            String pai = pais.get(i);
            if (pai == null) {
                continue;
            }
            if (!chaves.contains(chave(pai))) {
                throw new RegraDeNegocioException(
                    "O tipo pai '" + pai + "' de '" + nomes.get(i) + "' não existe no modelo");
            }
            if (chave(pai).equals(chave(nomes.get(i)))) {
                throw new RegraDeNegocioException("O tipo de unidade '" + nomes.get(i) + "' não pode ser pai de si próprio");
            }
        }
        Map<String, Integer> niveis = new HashMap<>();
        boolean progresso = true;
        while (progresso && niveis.size() < nomes.size()) {
            progresso = false;
            for (int i = 0; i < nomes.size(); i++) {
                String chaveDoTipo = chave(nomes.get(i));
                if (niveis.containsKey(chaveDoTipo)) {
                    continue;
                }
                String pai = pais.get(i);
                if (pai == null) {
                    niveis.put(chaveDoTipo, 1);
                    progresso = true;
                } else {
                    Integer nivelDoPai = niveis.get(chave(pai));
                    if (nivelDoPai != null) {
                        niveis.put(chaveDoTipo, nivelDoPai + 1);
                        progresso = true;
                    }
                }
            }
        }
        if (niveis.size() < nomes.size()) {
            for (int i = 0; i < nomes.size(); i++) {
                if (!niveis.containsKey(chave(nomes.get(i)))) {
                    throw new RegraDeNegocioException(
                        "A hierarquia tem um ciclo que envolve o tipo de unidade '" + nomes.get(i) + "'");
                }
            }
        }
        List<TipoResolvido> resolvidos = new ArrayList<>();
        int maximo = niveis.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        for (int nivel = 1; nivel <= maximo; nivel++) {
            for (int i = 0; i < nomes.size(); i++) {
                if (niveis.get(chave(nomes.get(i))) == nivel) {
                    resolvidos.add(new TipoResolvido(nomes.get(i), nivel, pais.get(i)));
                }
            }
        }
        return resolvidos;
    }
}
