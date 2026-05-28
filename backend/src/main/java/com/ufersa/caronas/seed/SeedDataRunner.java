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
 * o avaliador roda e ja tem usuarios + rotas suficientes para demonstrar o match.
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
        String UFERSA = "UFERSA";

        // Motoristas
        Usuario joao = usuarioService.salvar(new Usuario(
                "Joao Silva", "joao@ufersa.edu.br", "Ciencia da Computacao",
                "Nova Betania", UFERSA, true,
                new Veiculo("Honda Civic", "ABC-1234", "Prata", 3)));
        joao.registrarAvaliacao(4.8); joao.registrarAvaliacao(4.9); joao.registrarAvaliacao(5.0);

        Usuario maria = usuarioService.salvar(new Usuario(
                "Maria Souza", "maria@ufersa.edu.br", "Engenharia de Software",
                "Centro", UFERSA, true,
                new Veiculo("VW Gol", "DEF-5678", "Branco", 4)));
        maria.registrarAvaliacao(4.5); maria.registrarAvaliacao(4.7);

        Usuario pedro = usuarioService.salvar(new Usuario(
                "Pedro Costa", "pedro@ufersa.edu.br", "Agronomia",
                "Bom Jardim", UFERSA, true,
                new Veiculo("Fiat Uno", "GHI-9012", "Vermelho", 3)));
        pedro.registrarAvaliacao(4.2); pedro.registrarAvaliacao(4.0); pedro.registrarAvaliacao(4.5);

        Usuario carla = usuarioService.salvar(new Usuario(
                "Carla Lima", "carla@ufersa.edu.br", "Direito",
                "Alto de Sao Manoel", UFERSA, true,
                new Veiculo("Hyundai HB20", "JKL-3456", "Preto", 4)));
        carla.registrarAvaliacao(5.0); carla.registrarAvaliacao(4.9);

        Usuario rafael = usuarioService.salvar(new Usuario(
                "Rafael Mendes", "rafael@ufersa.edu.br", "Medicina Veterinaria",
                "Doze Anos", UFERSA, true,
                new Veiculo("Chevrolet Onix", "MNO-7890", "Cinza", 3)));
        rafael.registrarAvaliacao(4.6); rafael.registrarAvaliacao(4.8);

        // Passageiros
        usuarioService.salvar(new Usuario(
                "Ana Beatriz", "ana@ufersa.edu.br", "Ciencia da Computacao",
                "Nova Betania", UFERSA, false, null));
        usuarioService.salvar(new Usuario(
                "Lucas Pereira", "lucas@ufersa.edu.br", "Engenharia de Software",
                "Centro", UFERSA, false, null));
        usuarioService.salvar(new Usuario(
                "Beatriz Rocha", "bia@ufersa.edu.br", "Agronomia",
                "Aeroporto", UFERSA, false, null));
        usuarioService.salvar(new Usuario(
                "Felipe Alves", "felipe@ufersa.edu.br", "Direito",
                "Abolicao", UFERSA, false, null));

        // Rotas (manha - IDA para UFERSA)
        rotaService.salvar(new Rota(joao.getId(),    "Nova Betania",       UFERSA, LocalTime.of(7, 0),  3, TipoRota.IDA));
        rotaService.salvar(new Rota(maria.getId(),   "Centro",             UFERSA, LocalTime.of(7, 15), 4, TipoRota.IDA));
        rotaService.salvar(new Rota(pedro.getId(),   "Bom Jardim",         UFERSA, LocalTime.of(7, 30), 3, TipoRota.IDA));
        rotaService.salvar(new Rota(carla.getId(),   "Alto de Sao Manoel", UFERSA, LocalTime.of(6, 50), 4, TipoRota.IDA));
        rotaService.salvar(new Rota(rafael.getId(),  "Doze Anos",          UFERSA, LocalTime.of(7, 45), 3, TipoRota.IDA));

        // Rotas (tarde - VOLTA da UFERSA)
        rotaService.salvar(new Rota(joao.getId(),    "Nova Betania",       UFERSA, LocalTime.of(18, 0),  3, TipoRota.VOLTA));
        rotaService.salvar(new Rota(maria.getId(),   "Centro",             UFERSA, LocalTime.of(18, 30), 4, TipoRota.VOLTA));
        rotaService.salvar(new Rota(carla.getId(),   "Alto de Sao Manoel", UFERSA, LocalTime.of(17, 30), 4, TipoRota.VOLTA));

        System.out.println("===================================================");
        System.out.println("  Seed concluido: " + usuarioService.total()
                + " usuarios e " + rotaService.total() + " rotas cadastradas.");
        System.out.println("  Acesse http://localhost:8080/api/usuarios");
        System.out.println("===================================================");
    }
}
