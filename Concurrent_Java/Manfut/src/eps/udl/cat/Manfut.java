package eps.udl.cat;

public class Manfut {

    public static int numero_threads = 2;

    public static Thrd[] threads = null;
    public static int[] threads_return = null;

    public static void main(String[] args) {
        Market PlayersMarket;
        int PresupostFitxatges;
        JugadorsEquip MillorEquip;
        Error err;

        // Procesar argumentos.
        if (args.length < 2)
            throw new IllegalArgumentException("Error in arguments: ManFut <presupost> <fitxer_jugadors>");

        PresupostFitxatges = Integer.parseInt(args[0]);
        if (PresupostFitxatges <= 186){
            System.out.println("Pressupost baix podria donar errors, per defecte 200.");
            PresupostFitxatges = 200;
        }

        PlayersMarket = new Market();

        if (args.length > 2)
            numero_threads = Integer.parseInt(args[2]);

        numero_threads -= 1;

        if (numero_threads < 0) {
            System.out.println("Número de threads no vàlid: Per defecte 1+1\n");
            numero_threads = 1;
        }

        threads_return = new int[numero_threads + 1];
        threads = new Thrd[numero_threads];

        err = PlayersMarket.LlegirFitxerJugadors(args[1]);
        if (err != Error.COk)
            Error.showError("[Manfut] ERROR Reading players file.");

        // Calculate the best team.
        MillorEquip = PlayersMarket.CalcularEquipOptim(PresupostFitxatges);
        System.out.print(Error.color_blue);
        System.out.println("-- Best Team -------------------------------------------------------------------------------------");
        MillorEquip.PrintEquipJugadors();
        System.out.println("   Cost " + MillorEquip.CostEquip() + ", Points: " + MillorEquip.PuntuacioEquip() + ".");
        System.out.println("--------------------------------------------------------------------------------------------------");
        System.out.print(Error.end_color);

        System.exit(0);
    }


    // Methods Definition
    static int log(int x, int base) {
        return (int) Math.ceil((Math.log(x) / Math.log(base)));
    }

    static long log(long x, int base) {
        return (long) Math.ceil((Math.log(x) / Math.log(base)));
    }

    static int Log2(int x) {
        return (int) (log(x, 2));
    }

    static long Log2(long x) {
        return (long) (log(x, 2));
    }

    public static class Thrd extends Thread {
        public int first;
        public int end;
        public int presupost;
        public Market market;
        public int index;

        public Thrd(int first, int end, int presupost, Market market, int index) {
            this.first = first;
            this.end = end;
            this.presupost = presupost;
            this.market = market;
            this.index = index;
        }

        @Override
        public void run() {
            int equip;

            int MaxPuntuacio = -1;
            JugadorsEquip MillorEquip = null;

            // Evaluating different teams/combinations.
            System.out.println("Evaluating from " + String.format("%x", first) + "H to " + String.format("%x", end) + "H. Evaluating " + (end - first) + "  teams...");
            for (equip = this.first; equip <= this.end; equip++) {
                JugadorsEquip jugadors;

                // Get playes from team number. Returns false if the team is not valid.
                if ((jugadors = market.ObtenirJugadorsEquip(new IdEquip(equip))) == null)
                    continue;
                //System.out.print("Team " + equip + "->")
                // Reject teams with repeated players.
                if (jugadors.JugadorsRepetits()) {
                    //System.out.println(Error.color_red +" Invalid." + Error.end_color);
                    continue;    // Equip no valid.
                }

                // Chech if the team points is bigger than current optimal team, then evaluate if the cost is lower than the available budget
                if (jugadors.PuntuacioEquip() > MaxPuntuacio && jugadors.CostEquip() < presupost) {
                    // System.out.print("Team " + equip + "->");
                    // We have a new partial optimal team.
                    MaxPuntuacio = jugadors.PuntuacioEquip();
                    MillorEquip = jugadors;
                    Manfut.threads_return[this.index] = equip;
                    // System.out.println(MillorEquip);
                    // System.out.println(Error.color_green + " Cost: " + jugadors.CostEquip() + " Points: " + jugadors.PuntuacioEquip() + ". " + Error.end_color);
                }  //System.out.println(" Cost: " + jugadors.CostEquip() + " Points: " + jugadors.PuntuacioEquip() + ".\r");
            }
        }
    }
}
