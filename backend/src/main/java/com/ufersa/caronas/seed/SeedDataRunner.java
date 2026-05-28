package com.ufersa.caronas.seed;

import com.ufersa.caronas.model.Rota;
import com.ufersa.caronas.model.TipoRota;
import com.ufersa.caronas.model.Usuario;
import com.ufersa.caronas.model.Veiculo;
import com.ufersa.caronas.service.RotaService;
import com.ufersa.caronas.service.UsuarioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Popula dados de exemplo na inicializacao. Util para a apresentacao do projeto:
 * o avaliador roda e ja tem uma base "viva" de motoristas, passageiros e
 * rotas distribuidos em UFERSA, UERN e IFRN, com horarios de IDA e VOLTA.
 */
@Component
public class SeedDataRunner implements CommandLineRunner {

    private final UsuarioService usuarioService;
    private final RotaService rotaService;

    public SeedDataRunner(UsuarioService usuarioService, RotaService rotaService) {
        this.usuarioService = usuarioService;
        this.rotaService = rotaService;
    }

    @Override
    public void run(String... args) {

        // ==================================================== UFERSA
        Usuario joao = motorista("Joao Silva", "joao@ufersa.edu.br",
                "Ciencia da Computacao", "Nova Betania", "UFERSA",
                veiculo("Honda Civic", "ABC-1234", "Prata", 3));
        avaliar(joao, 4.8, 4.9, 5.0);

        Usuario maria = motorista("Maria Souza", "maria@ufersa.edu.br",
                "Engenharia de Software", "Centro", "UFERSA",
                veiculo("VW Gol", "DEF-5678", "Branco", 4));
        avaliar(maria, 4.5, 4.7);

        Usuario pedro = motorista("Pedro Costa", "pedro@ufersa.edu.br",
                "Agronomia", "Bom Jardim", "UFERSA",
                veiculo("Fiat Uno", "GHI-9012", "Vermelho", 3));
        avaliar(pedro, 4.2, 4.0, 4.5);

        Usuario carla = motorista("Carla Lima", "carla@ufersa.edu.br",
                "Direito", "Alto de Sao Manoel", "UFERSA",
                veiculo("Hyundai HB20", "JKL-3456", "Preto", 4));
        avaliar(carla, 5.0, 4.9);

        Usuario rafael = motorista("Rafael Mendes", "rafael@ufersa.edu.br",
                "Medicina Veterinaria", "Doze Anos", "UFERSA",
                veiculo("Chevrolet Onix", "MNO-7890", "Cinza", 3));
        avaliar(rafael, 4.6, 4.8);

        Usuario thiago = motorista("Thiago Ribeiro", "thiago@ufersa.edu.br",
                "Engenharia Mecanica", "Aeroporto", "UFERSA",
                veiculo("Toyota Etios", "PQR-2345", "Branco", 4));
        avaliar(thiago, 4.7, 4.6, 4.9, 4.8);

        passageiro("Ana Beatriz", "ana@ufersa.edu.br",
                "Ciencia da Computacao", "Nova Betania", "UFERSA");
        passageiro("Lucas Pereira", "lucas@ufersa.edu.br",
                "Engenharia de Software", "Centro", "UFERSA");
        passageiro("Beatriz Rocha", "bia@ufersa.edu.br",
                "Agronomia", "Aeroporto", "UFERSA");
        passageiro("Felipe Alves", "felipe@ufersa.edu.br",
                "Direito", "Abolicao", "UFERSA");

        // ==================================================== UERN
        Usuario juliana = motorista("Juliana Cavalcanti", "juliana@uern.br",
                "Letras", "Centro", "UERN",
                veiculo("Renault Sandero", "STU-1111", "Vermelho", 4));
        avaliar(juliana, 4.9, 5.0, 4.8);

        Usuario marcos = motorista("Marcos Vieira", "marcos@uern.br",
                "Historia", "Costa e Silva", "UERN",
                veiculo("Ford Ka", "VWX-2222", "Azul", 3));
        avaliar(marcos, 4.4, 4.5);

        Usuario isabela = motorista("Isabela Nunes", "isabela@uern.br",
                "Pedagogia", "Nova Betania", "UERN",
                veiculo("VW Voyage", "YZA-3333", "Prata", 4));
        avaliar(isabela, 4.7, 4.6);

        passageiro("Gabriel Lopes", "gabriel@uern.br",
                "Geografia", "Alto de Sao Manoel", "UERN");
        passageiro("Mariana Diniz", "mariana@uern.br",
                "Letras", "Centro", "UERN");
        passageiro("Henrique Sa", "henrique@uern.br",
                "Quimica", "Doze Anos", "UERN");

        // ==================================================== IFRN
        Usuario daniela = motorista("Daniela Freitas", "daniela@ifrn.edu.br",
                "Tecnologia da Informacao", "Bom Jardim", "IFRN",
                veiculo("Jeep Renegade", "BCD-4444", "Preto", 4));
        avaliar(daniela, 5.0, 4.9, 5.0, 4.7);

        Usuario otavio = motorista("Otavio Bezerra", "otavio@ifrn.edu.br",
                "Eletrotecnica", "Belo Horizonte", "IFRN",
                veiculo("Chevrolet Prisma", "EFG-5555", "Branco", 3));
        avaliar(otavio, 4.3, 4.6);

        passageiro("Larissa Cunha", "larissa@ifrn.edu.br",
                "Informatica", "Nova Betania", "IFRN");
        passageiro("Eduardo Rocha", "eduardo@ifrn.edu.br",
                "Mecatronica", "Centro", "IFRN");
        passageiro("Camila Torres", "camila@ifrn.edu.br",
                "Quimica Industrial", "Bom Jardim", "IFRN");

        // ==================================================== Rotas
        // UFERSA - IDA manha
        rota(joao,    "Nova Betania",       "UFERSA", "07:00", 3, TipoRota.IDA);
        rota(maria,   "Centro",             "UFERSA", "07:15", 4, TipoRota.IDA);
        rota(pedro,   "Bom Jardim",         "UFERSA", "07:30", 3, TipoRota.IDA);
        rota(carla,   "Alto de Sao Manoel", "UFERSA", "06:50", 4, TipoRota.IDA);
        rota(rafael,  "Doze Anos",          "UFERSA", "07:45", 3, TipoRota.IDA);
        rota(thiago,  "Aeroporto",          "UFERSA", "07:20", 4, TipoRota.IDA);

        // UFERSA - IDA tarde (turno vespertino)
        rota(maria,   "Centro",             "UFERSA", "13:00", 4, TipoRota.IDA);
        rota(rafael,  "Doze Anos",          "UFERSA", "13:15", 3, TipoRota.IDA);

        // UFERSA - VOLTA
        rota(joao,    "Nova Betania",       "UFERSA", "18:00", 3, TipoRota.VOLTA);
        rota(maria,   "Centro",             "UFERSA", "18:30", 4, TipoRota.VOLTA);
        rota(carla,   "Alto de Sao Manoel", "UFERSA", "17:30", 4, TipoRota.VOLTA);
        rota(thiago,  "Aeroporto",          "UFERSA", "18:15", 4, TipoRota.VOLTA);
        rota(pedro,   "Bom Jardim",         "UFERSA", "22:00", 3, TipoRota.VOLTA);
        rota(rafael,  "Doze Anos",          "UFERSA", "22:15", 3, TipoRota.VOLTA);

        // UERN - IDA manha + noite
        rota(juliana, "Centro",             "UERN",   "07:30", 4, TipoRota.IDA);
        rota(marcos,  "Costa e Silva",      "UERN",   "07:45", 3, TipoRota.IDA);
        rota(isabela, "Nova Betania",       "UERN",   "07:00", 4, TipoRota.IDA);
        rota(juliana, "Centro",             "UERN",   "18:30", 4, TipoRota.IDA);   // turno noturno
        rota(marcos,  "Costa e Silva",      "UERN",   "19:00", 3, TipoRota.IDA);

        // UERN - VOLTA
        rota(juliana, "Centro",             "UERN",   "12:00", 4, TipoRota.VOLTA);
        rota(isabela, "Nova Betania",       "UERN",   "12:15", 4, TipoRota.VOLTA);
        rota(juliana, "Centro",             "UERN",   "22:30", 4, TipoRota.VOLTA);

        // IFRN - IDA manha + tarde + noite
        rota(daniela, "Bom Jardim",         "IFRN",   "07:00", 4, TipoRota.IDA);
        rota(otavio,  "Belo Horizonte",     "IFRN",   "07:30", 3, TipoRota.IDA);
        rota(daniela, "Bom Jardim",         "IFRN",   "13:00", 4, TipoRota.IDA);
        rota(otavio,  "Belo Horizonte",     "IFRN",   "18:45", 3, TipoRota.IDA);

        // IFRN - VOLTA
        rota(daniela, "Bom Jardim",         "IFRN",   "12:00", 4, TipoRota.VOLTA);
        rota(otavio,  "Belo Horizonte",     "IFRN",   "22:30", 3, TipoRota.VOLTA);

        System.out.println("===========================================================");
        System.out.println("  Seed concluido: " + usuarioService.total()
                + " usuarios e " + rotaService.total() + " rotas cadastradas.");
        System.out.println("  Universidades: UFERSA, UERN, IFRN");
        System.out.println("  Acesse:  http://localhost:8080/api/usuarios");
        System.out.println("  Bairros: http://localhost:8080/api/bairros");
        System.out.println("===========================================================");
    }

    // ===================================================== helpers
    private Veiculo veiculo(String modelo, String placa, String cor, int vagas) {
        return new Veiculo(modelo, placa, cor, vagas);
    }

    private Usuario motorista(String nome, String email, String curso,
                              String bairro, String universidade, Veiculo veiculo) {
        return usuarioService.salvar(
                new Usuario(nome, email, curso, bairro, universidade, true, veiculo));
    }

    private Usuario passageiro(String nome, String email, String curso,
                               String bairro, String universidade) {
        return usuarioService.salvar(
                new Usuario(nome, email, curso, bairro, universidade, false, null));
    }

    private void avaliar(Usuario u, double... notas) {
        for (double n : notas) u.registrarAvaliacao(n);
    }

    private void rota(Usuario u, String origem, String destino,
                      String horario, int vagas, TipoRota tipo) {
        rotaService.salvar(new Rota(u.getId(), origem, destino,
                LocalTime.parse(horario), vagas, tipo));
    }
}
