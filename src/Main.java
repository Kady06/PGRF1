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
                  F – režim semínkového vyplnění
                  T – přepínač podmínky výplně (barva pozadí / barva hranice)
                  C – smazat plátno a všechna data
                  LMB – úsečka / vrchol / v režimu F kreslení hranice
                  Shift+LMB (úsečka) – vodorovná / svislá / úhlopříčná (45°)
                  RMB – přesun vrcholu (P) / semínko výplně (F)
                """);
    }
}
