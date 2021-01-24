package eps.udl.cat;

import java.util.ArrayList;
import java.io.*;


public class Market {
    ArrayList<Jugador> Jugadors;

    int     NJugadors;
    int     NPorters;
    int     NDefensors;
    int     NMitjos;
    int     NDelanters;

    Market() {
        Jugadors = new ArrayList<Jugador>();
    }

    Jugador GetJugador(int j) { return Jugadors.get(j); }
    Jugador GetPorter(int j) { return Jugadors.get(j); }
    Jugador GetDefensor(int j) { return Jugadors.get(NPorters+j); }
    Jugador GetMitg(int j) { return Jugadors.get(NPorters+NDefensors+j); }
    Jugador GetDelanter(int j) { return Jugadors.get(NPorters+NDefensors+NMitjos+j); }

    // Read file with the market players list (each line containts a plater: "Id.;Name;Position;Cost;Team;Points")
    Error LlegirFitxerJugadors(String pathJugadors)
    {
        long Offset=0;
        FileInputStream fis;

        try {
            fis = new FileInputStream(pathJugadors);
        } catch (FileNotFoundException e) {
            System.err.println("[Market:LlegirFitxerJugadors] File "+pathJugadors+" not found.");
            e.printStackTrace();
            return(Error.CErrorOpenInputFile);
        }

        try {
            //Construct BufferedReader from InputStreamReader
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            br.readLine(); // skip csv header

            // Read players.
            NJugadors=NPorters=NDefensors=NMitjos=NDelanters=0;
            String line = null;
            while ((line = br.readLine())!=null)
            {
                String fields[] = line.split(";");
                Jugador player = new Jugador();

                // Player's identificator
                player.setId(Integer.parseInt(fields[0]));

                // Player's name
                player.setNom(fields[1]) ;

                // Player's position
                switch(fields[2]) {
                    case "Portero":
                        player.setTipus(TipusJug.JPorter);
                        NPorters++;
                        break;

                    case "Defensa":
                        player.setTipus(TipusJug.JDefensor);
                        NDefensors++;
                        break;

                    case "Medio":
                        player.setTipus(TipusJug.JMitg);
                        NMitjos++;
                        break;

                    case "Delantero":
                        player.setTipus(TipusJug.JDelanter);
                        NDelanters++;
                        break;

                    default:
                        System.err.println("[Market:LlegirFitxerJugadors] Error player type.");
                        return(Error.CErrorPlayerType);
                }

                // Player's cost
                player.setCost(Integer.parseInt(fields[3]));

                // Player's team
                player.setEquip(fields[4]); ;

                // Player's points
                player.setPunts(Integer.parseInt(fields[5]));

                NJugadors++;
                Jugadors.add(player);
            }
            br.close();

        } catch (IOException e) {
            System.err.println("[Market:LlegirFitxerJugadors] Reading file "+pathJugadors+".");
            e.printStackTrace();
            return(Error.CErrorReadingFile);
        }

        return(Error.COk);
    }


    JugadorsEquip  CalcularEquipOptim(int PresupostFitxatges)
    {
        long maxbits;
        int equip, primerEquip, ultimEquip, first, end;
        int MaxPuntuacio=-1;
        JugadorsEquip MillorEquip = null;

        //PrintJugadors();

        // Calculated number of bits required for all teams codification.
        maxbits=Manfut.Log2(NPorters)*JugadorsEquip.DPosPorters+Manfut.Log2(NDefensors)*JugadorsEquip.DPosDefensors+Manfut.Log2(NMitjos)*JugadorsEquip.DPosMitjos+Manfut.Log2(NDelanters)*JugadorsEquip.DPosDelanters;
        if (maxbits>Manfut.Log2(Long.MAX_VALUE))
            Error.showError("[Market:CalcularEquipOptim] The number of player overflow the maximum width supported.");

        // Calculate first and end team that have to be evaluated.
        first=primerEquip=GetEquipInicial();
        end=ultimEquip=(int)Math.pow(2,maxbits);

        System.out.println("TOTAL: First: " + String.format("%x",first) + "H End: " + String.format("%x",end) + "H\n");

        int combinacions_per_thread = ((end - first) / (Manfut.numero_threads + 1));

        for(int i = 0; i < Manfut.numero_threads; i++){
            if(i == 0){
                Manfut.threads[i] = new Manfut.Thrd(first, first + combinacions_per_thread, PresupostFitxatges, this, i);
            }else{
                Manfut.threads[i] = new Manfut.Thrd(Manfut.threads[i-1].end, Manfut.threads[i-1].end + combinacions_per_thread, PresupostFitxatges, this, i);
            }
            Manfut.threads[i].start();
        }
        Manfut.Thrd main_thrd;
        if (Manfut.numero_threads == 0){
            main_thrd = new Manfut.Thrd(first, first + combinacions_per_thread, PresupostFitxatges, this, Manfut.numero_threads);
        }else{
            main_thrd = new Manfut.Thrd(Manfut.threads[Manfut.numero_threads-1].end, Manfut.threads[Manfut.numero_threads-1].end + combinacions_per_thread, PresupostFitxatges, this,Manfut.numero_threads);
        }
        main_thrd.run();
        MillorEquip = this.ObtenirJugadorsEquip(new IdEquip(Manfut.threads_return[Manfut.numero_threads]));
        // System.out.println(MillorEquip);
        for(int i = 0; i < Manfut.numero_threads;i++){
            try {
                Manfut.threads[i].join();
                // System.out.println("Joined");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // System.out.println(Manfut.threads_return[i]);
            JugadorsEquip equip_retornat = this.ObtenirJugadorsEquip(new IdEquip(Manfut.threads_return[i]));
            if (equip_retornat.PuntuacioEquip() > MillorEquip.PuntuacioEquip()){
                MillorEquip = equip_retornat;
            }
        }
        return MillorEquip;
    }


    // Calculate the initial team combination.
    int GetEquipInicial()
    {
        int p, equip=0;
        int bitsPorters, bitsDefensors, bitsMitjos, bitsDelanters;

        bitsPorters = Manfut.Log2(NPorters);
        bitsDefensors = Manfut.Log2(NDefensors);
        bitsMitjos = Manfut.Log2(NMitjos);
        bitsDelanters = Manfut.Log2(NDelanters);

        for (p=JugadorsEquip.DPosDelanters-1;p>=0;p--)
        {
            equip+=p;
            equip = equip << bitsDelanters;
        }

        for (p=JugadorsEquip.DPosMitjos-1;p>=0;p--)
        {
            equip+=p;
            equip = equip << bitsMitjos;
        }

        for (p=JugadorsEquip.DPosDefensors-1;p>=0;p--)
        {
            equip+=p;
            equip = equip << bitsDefensors;
        }

        for (p=JugadorsEquip.DPosPorters-1;p>0;p--)
        {
            equip+=p;
            equip = equip << bitsPorters;
        }

        return (equip);
    }


    // Convert team combination to an struct with all the player by position.
    // Returns false if the team is not valid.
    JugadorsEquip ObtenirJugadorsEquip (IdEquip equip)
    {
        int p;
        int bitsPorters, bitsDefensors, bitsMitjos, bitsDelanters;
        JugadorsEquip jugadors=new JugadorsEquip(this);

        bitsPorters = Manfut.Log2(NPorters);
        bitsDefensors = Manfut.Log2(NDefensors);
        bitsMitjos = Manfut.Log2(NMitjos);
        bitsDelanters = Manfut.Log2(NDelanters);

        for (p=0;p<JugadorsEquip.DPosPorters;p++)
        {
            jugadors.setPorter((equip.getIdEquip()>>(bitsPorters*p)) & ((int)Math.pow(2,bitsPorters)-1));
            if (jugadors.getPorter()>=NPorters)
                return null;
        }

        for (p=0;p<JugadorsEquip.DPosDefensors;p++)
        {
            jugadors.setDefensor(p, (equip.getIdEquip()>>((bitsPorters*JugadorsEquip.DPosPorters)+(bitsDefensors*p))) & ((int)Math.pow(2,bitsDefensors)-1));
            if (jugadors.getDefensor(p)>=NDefensors)
                return null;
        }

        for (p=0;p<JugadorsEquip.DPosMitjos;p++)
        {
            jugadors.setMitg(p, (equip.getIdEquip()>>((bitsPorters*JugadorsEquip.DPosPorters)+(bitsDefensors*JugadorsEquip.DPosDefensors)+(bitsMitjos*p))) & ((int)Math.pow(2,bitsMitjos)-1));
            if (jugadors.getMitg(p)>=NMitjos)
                return null;
        }

        for (p=0;p<JugadorsEquip.DPosDelanters;p++)
        {
            jugadors.setDelanter(p,(equip.getIdEquip()>>((bitsPorters*JugadorsEquip.DPosPorters)+(bitsDefensors*JugadorsEquip.DPosDefensors)+(bitsMitjos*JugadorsEquip.DPosMitjos)+(bitsDelanters*p))) & ((int)Math.pow(2,bitsDelanters)-1));
            if (jugadors.getDelanter(p)>=NDelanters)
                return null;
        }

        return jugadors;
    }


    // Prints all market players information,
    void PrintJugadors()
    {
        int j;

        for(j=0;j<NJugadors;j++)
        {
            GetJugador(j).print();
        }
    }

}
