package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;
import jakarta.persistence.OrderBy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";

    List<DadosSerie> dadosSeries = new ArrayList<>();

    private SerieRepository repositorio;

    private List<Serie> series = new ArrayList<>();

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void exibeMenu() {



            var opcao = -1;

            while (opcao != 0){
                var menu = """
                1 - Buscar séries
                2 - Buscar episódios
                3 - Series Listadas
                4 - Buscar serie por titulo
                5 - Buscar series por Ator
                6 - Buscar Top 5
                7 - Buscar por categoria
                
                0 - Sair                                 
                """;

                System.out.println(menu);
                opcao = leitura.nextInt();
                leitura.nextLine();

                switch (opcao) {
                    case 1:
                        buscarSerieWeb();
                        break;
                    case 2:
                        buscarEpisodioPorSerie();
                        break;
                    case 3:
                        mostrarSeriesListadas();
                        break;
                    case 4:
                        buscarSeriePorTitulo();
                        break;
                    case 5:
                        buscarSeriesPorAtor();
                        break;
                    case 6:
                        buscarTopCinco();
                        break;
                    case 7:
                        buscarSeriesPorCategoria();

                        break;
                    case 8:
                        buscarPorQuantidadeTemp();
                        break;
                    case 0:
                        System.out.println("Saindo...");
                        break;
                    default:
                        System.out.println("Opção inválida");
                }
            }
            }




    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        //dadosSeries.add(dados);
        Serie serie = new Serie(dados);
        repositorio.save(serie);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        mostrarSeriesListadas();
        System.out.println("Escolha uma serie pelo nome: ");
        var nomeSerie = leitura.nextLine().toLowerCase();

        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);
        if(serie.isPresent()){

            var serieEncontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        } else {
            System.out.println("Séria não encontrada");
        }


    }

    private void mostrarSeriesListadas(){

            series = repositorio.findAll();
            //pega do repositorio e tras para a gente na aplicação

            series.stream()
                            .sorted(Comparator.comparing(Serie::getTitulo))
                    .forEach(System.out::println);
    }
    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma serie pelo nome: ");
        var nomeSerie = leitura.nextLine().toLowerCase();
        Optional<Serie> serieBuscada = repositorio.findByTituloContainingIgnoreCase(nomeSerie);
        if(serieBuscada.isPresent()){
            System.out.println("Dados da séria: " + serieBuscada.get());

        }else {
            System.out.println("Serie não encontrada!!");
        }

    }

    private void buscarSeriesPorAtor() {
        System.out.println("Qual o nome para a Busca: ");
        var nomeAtor = leitura.nextLine();
        System.out.println("Qual avaliação minima: ");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEcontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);
        seriesEcontradas.forEach(s -> System.out.println(s.getTitulo()));
    }

    private void buscarTopCinco() {
        //esse metodo foi criado por mim e é mais facil que parece
        List<Serie> topSeries = repositorio.findTop5ByOrderByAvaliacaoDesc();
        topSeries.forEach(s -> System.out.println("Serie: " +s.getTitulo() + " - " + s.getAvaliacao()));
    }

    private void buscarSeriesPorCategoria(){
        System.out.println("Deseja buscar uma serie de qual categoria? ");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);//método criado acima
        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
        System.out.println("Séries da categoria " + nomeGenero);
        seriesPorCategoria.forEach(System.out::println);
    }
    //Metodo do desafio
    private void buscarPorQuantidadeTemp() {
        System.out.println("Escolha a quantidade maxima de temporadas: ");
        Integer totalTemporadas = leitura.nextInt();
        System.out.println("Qual avaliação minima: ");
        var avaliacao = leitura.nextDouble();
        List<Serie> SeriesTemp = repositorio.seriesPorTemporadaEAvaliacao(totalTemporadas,  avaliacao);
        SeriesTemp.forEach(s -> System.out.println(s.getTitulo()));
    }

}

