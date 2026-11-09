import controller.Controller2D;
import view.Window;

public class Main {

    public static void main(String[] args) {
        Window window = new Window(800, 600);
        new Controller2D(window.getPanel());

        System.out.println("""
                Ovládání:
                  L – režim úsečky (výchozí)
                  P – režim n-úhelníku
                  C – smazat plátno a všechna data
                  LMB – úsečka / přidání vrcholu (od 2 bodů do nejbližší hrany)
                  RMB – uchopit nejbližší vrchol a tažením přesunout
                """);
    }
}
