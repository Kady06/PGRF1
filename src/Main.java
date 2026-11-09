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
                  S – scan-line vyplnění n-úhelníku pod kurzorem
                  X – zapnout / vypnout ořezání n-úhelníku (Sutherland–Hodgman)
                  O – otočit orientaci ořezávacího polygonu (CW/CCW)
                  Y – zapnout / vypnout ořezání úseček (Cohen–Sutherland)
                  Enter – uložit n-úhelník a začít nový
                  C – smazat plátno, reset stavů a všech datových struktur
                  LMB – úsečka / vrchol / v režimu F kreslení hranice
                  Shift+LMB (úsečka) – vodorovná / svislá / úhlopříčná (45°)
                  RMB – přesun vrcholu (P) / semínko výplně (F)
                """);
    }
}
