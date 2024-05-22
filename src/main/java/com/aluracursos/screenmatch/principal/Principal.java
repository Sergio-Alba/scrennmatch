package com.aluracursos.screenmatch.principal;

import com.aluracursos.screenmatch.model.Categoria;
import com.aluracursos.screenmatch.model.DatosSerie;
import com.aluracursos.screenmatch.model.DatosTemporada;
import com.aluracursos.screenmatch.model.Serie;
import com.aluracursos.screenmatch.repository.SerieRepository;
import com.aluracursos.screenmatch.services.ConsumoApi;
import com.aluracursos.screenmatch.services.ConvierteDatos;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToFile;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private ConsumoApi consumoApi = new ConsumoApi();
    private final String URL_BASE = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=4fc7c187";
    private ConvierteDatos conversor = new ConvierteDatos();
    private List<DatosSerie> datosSeries = new ArrayList<>();
    private SerieRepository repositorio;
    private List<Serie> series;
    private Optional<Serie> serieBusqueda;

    public Principal(SerieRepository repository) {
        this.repositorio=repository;
    }

    public void mostrarElMenu(){
        var option = -1;
        while (option != 0){
            var menu = """
                    1 - Buscar series
                    2 - Buscar Episodios
                    3 - Mostrar busquedas
                    4 - Buscar series por el titulo
                    5 - Buscar top 5 series
                    6 - Buscar por categoria
                    7 - Buscar series por temporadas
                    8 - Buscar Episodio por titulo
                    9 - Top 5 Episodios por Serie
                    
                    0 - Salir
                    """;
            System.out.println(menu);
            option = teclado.nextInt();
            teclado.nextLine();

            switch (option){
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    mostrarSeriesBuscadas();
                    break;
                case 4 :
                    buscarSeriesPorTitulo();
                    break;
                case 5 :
                    buscarTop5SeriesMejorEvaluadas();
                    break;
                case 6 :
                    buscarSeriesPorCategoria();
                    break;
                case 7 :
                    filtrarSeriesPorTemporadaYEvaluacion();
                    break;
                case 8 :
                    buscarEpisodioPorTitulo();
                    break;
                case 9 :
                    buscarTop5Episodios();
                    break;
                case 0:
                    System.out.println("Cerrando la aplicación...");
                    break;
                default:
                    System.out.println("Opcion no valida.");
                    break;
            }
        }
    }




    private DatosSerie getDatosSerie() {
        System.out.println("Escribe el nombre de la serie que deseas buscar");
        var nombreSerie = teclado.nextLine();
        var json = consumoApi.obtenerDatos(URL_BASE + nombreSerie.replaceAll(" ","+") + API_KEY);
        DatosSerie datos = conversor.obtenerDatos(json, DatosSerie.class);
        System.out.println(datos.sinopsis());
        return datos;
    }
    private void buscarEpisodioPorSerie() {
        mostrarSeriesBuscadas();
        System.out.println("Escribe el nombre de la serie que quieres ver los episodios");
        var nombreSerie = teclado.nextLine();

        Optional<Serie> serie = series.stream()
                .filter(s -> s.getTitulo().toLowerCase().contains(nombreSerie.toLowerCase()))
                .findFirst();

        if (serie.isPresent()){
            var serieEncontrada = serie.get();
            List<DatosTemporada> temporadas = new ArrayList<>();
            for (int i = 1; i <= serieEncontrada.getTotalDeTemporadas() ; i++) {
                var json = consumoApi.obtenerDatos(URL_BASE + serieEncontrada.getTitulo().replace(" ","+") + "&season=" + i + API_KEY);
                DatosTemporada datosTemporada = conversor.obtenerDatos(json,DatosTemporada.class);
                temporadas.add(datosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                .flatMap(d -> d.episodios().stream()
                        .map(e -> new Episodio(d.numero(),e)))
                .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        }
    }
    private void buscarSerieWeb(){
        DatosSerie datos = getDatosSerie();
        Serie serie = new Serie(datos);
        repositorio.save(serie);
        // datosSeries.add(datos);
        System.out.println(datos);

    }
    private void mostrarSeriesBuscadas() {
        series = repositorio.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);

    }
    private void buscarSeriesPorTitulo(){
        System.out.println("Escribe el nombre de la serie que deseas buscar:");
        var nombreSerie = teclado.nextLine();
        serieBusqueda = repositorio.findByTituloContainsIgnoreCase(nombreSerie);

        if (serieBusqueda.isPresent()){
            System.out.println("La Serie buscada es: "+ serieBusqueda.get());
        }else {
            System.out.println("Serie no encontrada.");
        }
    }
    private void buscarTop5SeriesMejorEvaluadas(){
        List<Serie> topSeries = repositorio.findTop5ByOrderByEvaluacionDesc();
        topSeries.forEach(s -> System.out.println("Serie: " + s.getTitulo() + " Evaluación: " + s.getEvaluacion()));
    }
    private void buscarSeriesPorCategoria(){
        System.out.println("Escriba el genero/Categoria de la serie que desea buscar ");
        var genero = teclado.nextLine();
        var categoria = Categoria.fromEspaniol(genero);
        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
        System.out.println("Las series de la categoria " + genero);
        seriesPorCategoria.forEach(System.out::println);
    }
    public void filtrarSeriesPorTemporadaYEvaluacion(){
        System.out.println("¿Filtrar séries con cuántas temporadas? ");
        var totalDeTemporadas = teclado.nextInt();
        teclado.nextLine();
        System.out.println("¿Com evaluación apartir de cuál valor? ");
        var evaluacion = teclado.nextDouble();
        teclado.nextLine();
        List<Serie> filtroSeries = repositorio.seriesPorTemporadayEvaluacion(totalDeTemporadas,evaluacion);
        System.out.println("*** Series filtradas ***");
        filtroSeries.forEach(s ->
                System.out.println("Serie: " + s.getTitulo() + "  - evaluacion: " + s.getEvaluacion() + " Temporadas: " + s.getTotalDeTemporadas()));
    }
    private void buscarEpisodioPorTitulo(){
        System.out.println("Escribe el nombre del episodio que deseas buscar");
        var nombreEpisodio = teclado.nextLine();
        List<Episodio> EpisodiosPorNombre = repositorio.episodiosPorNombre(nombreEpisodio);
        EpisodiosPorNombre.forEach(e -> System.out.printf("Serie: %s temporada %s Episodio %s Evaluacion %s",
                e.getSerie(), e.getTemporada(), e.getNumeroEpisodio(), e.getEvaluacion()));
    }
    private void buscarTop5Episodios(){
        buscarSeriesPorTitulo();
        if(serieBusqueda.isPresent()){
            Serie serie = serieBusqueda.get();
            List<Episodio> topEpisodios = repositorio.top5Episodios(serie);
            topEpisodios.forEach(e -> System.out.printf("Serie: %s - temporada %s - Episodio %s - Titulo %s - Evaluacion %s",
                    e.getSerie(), e.getTemporada(), e.getNumeroEpisodio(), e.getTitulo(), e.getEvaluacion()));
        }
    }
}




























