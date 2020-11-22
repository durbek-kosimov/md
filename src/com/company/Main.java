package com.company;

import java.io.*;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.InputMismatchException;
import java.util.StringTokenizer;
//1) класс должен заниматься одним конкретным делом, то есть если уж он у тебя занимается расчетами,
//   то не должен принимать ввод юзера, пусть ему параметры передают в конструктор или метод.
//   А он пусть возвращает значение, а не пишет
//2) весь ввод выносим в один класс, сканнер должен быть как горец - только 1
public class Main {
    static double deltaT, density, temperature, rCut, velMag, timeNow, uSum, vvSum;
    static double dispHi, rNebrShell;
    static int nebrNow, nebrTabFac, nebrTabLen, nebrTabMax;
    static int stepAvg, stepEquil, stepLimit, nMol, stepCount;

    public static void main(String[] args) throws IOException {

        final int NDIM = 3;
        boolean moreCycles;

        int stepAvg, stepEquil, stepLimit, nMol, stepCount;
        double kinEnInitSum;
        double pertTrajDev;
        int stepInitlzTemp, randSeed;
        int countTrajDev, limitTrajDev, stepTrajDev;
        Prop kinEnergy = new Prop();
        Prop totEnergy = new Prop();
        Prop pressure = new Prop();

//        File inFile = new File("/home/dmint/Desktop/pr_03_5.in");
        File inFile = new File("pr_03_2.in");
        File outFile1 = new File("coords.d");
        File outFile2 = new File("gpcoords.d");
        File outFile3 = new File("velo.d");
        File outFile4 = new File("veloAvg.d");
        File outFile5 = new File("coordsStep1.d");
        File outFile6 = new File("veloStep1.d");


        BufferedReader in = new BufferedReader(new FileReader(inFile));
        PrintWriter out1 = new PrintWriter(new BufferedWriter(new FileWriter(outFile1))); //jmol coords.d
        PrintWriter out2 = new PrintWriter(new BufferedWriter(new FileWriter(outFile2))); //gpcoords.d
        PrintWriter out3 = new PrintWriter(new BufferedWriter(new FileWriter(outFile3))); //gnuplot
        PrintWriter out4 = new PrintWriter(new BufferedWriter(new FileWriter(outFile4))); //gnuplot
        PrintWriter out5 = new PrintWriter(new BufferedWriter(new FileWriter(outFile5))); //gnuplot
        PrintWriter out6 = new PrintWriter(new BufferedWriter(new FileWriter(outFile6))); //gnuplot

        ArrayList<NameI> nameI = new ArrayList<>();
        ArrayList<NameR> nameR = new ArrayList<>();
        ArrayList<Integer> cellList = new ArrayList<>();
        ArrayList<Integer> nebrTab = new ArrayList<>();
        ArrayList<Double> valTrajDev = new ArrayList<>();
        ArrayList<Mol> mol = new ArrayList<>();
        InitUcell initUcell = new InitUcell();

        VeloSum veloSum = new VeloSum();
        CalcMet calcMet = new CalcMet();
        String line;
        String value = "";
        String description = "";
        String x = "", y = "", z = "";
        int countLine = 1;
        int countToken = 0;
        int flag = 0;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            StringTokenizer stLine = new StringTokenizer(line, " ");
            countToken = stLine.countTokens();
            switch (countToken) {
                case 1:
                    throw new InputMismatchException("Incopatible data!!! In line " + countLine);
                case 2:
                    description = stLine.nextToken();
                    if (description.equals("initUcell"))
                        throw new InputMismatchException("Few data! In line " + countLine);
                    value = stLine.nextToken();
                    if (value.contains(".") || value.contains("e-")) {
                        flag = 1;
                        nameR.add(new NameR(description, Double.parseDouble(value), flag));
                    }
                    else {
                        nameI.add(new NameI(description, Integer.parseInt(value)));
                    }
                    break;
                case 3:
                    description = stLine.nextToken();
                    if (description.equals("initUcell")) {
                        while (stLine.hasMoreTokens()) {
                            x = stLine.nextToken();
                            y = stLine.nextToken();
                        }
                        if (x.contains(".") || y.contains(".")) {
                            throw new InputMismatchException("Parameters of initUcell must be integers!");
                        }
                        initUcell.x = Integer.parseInt(x);
                        initUcell.y = Integer.parseInt(y);
                    }
                    else {
                        throw new InputMismatchException("Too many data! In line " + countLine);
                    }
                    break;
                case 4:
                    description = stLine.nextToken();
                    if (description.equals("initUcell")) {
                        while (stLine.hasMoreTokens()) {
                            x = stLine.nextToken();
                            y = stLine.nextToken();
                            z = stLine.nextToken();
                        }
                        if (x.contains(".") || y.contains(".") || z.contains(".")) {
                            throw new InputMismatchException("Parameters of initUcell must be integers!");
                        }
                        initUcell.x = Integer.parseInt(x);
                        initUcell.y = Integer.parseInt(y);
                        initUcell.z = Integer.parseInt(z);
                    }
                    else {
                        throw new InputMismatchException("Too many data! In line " + countLine);
                    }
                    break;
                case 5:
                    throw new InputMismatchException("Too many data! In line " + countLine);
            }
            if (countToken == 2) {
                System.out.println(description + "\t\t" + value);
            }
            else if (countToken == 3) {
                System.out.println(description + "\t" + x + " " + y);
            }
            else if (countToken == 4) {
                System.out.println(description + "\t" + x + " " + y + " " + z);
            }
            countLine ++;
            flag = 0;
        }
        in.close();
        for (int i = 0; i < nameR.size(); i ++) {
            switch (nameR.get(i).getvName()) {
                case "deltaT":
                    deltaT = nameR.get(i).getvValue();
                    break;
                case "density":
                    density = nameR.get(i).getvValue();
                    break;
                case "temperature":
                    temperature = nameR.get(i).getvValue();
                    break;
                case "pertTrajDev":
                    pertTrajDev = nameR.get(i).getvValue();
                    break;
                case "rNebrShell":
                    rNebrShell = nameR.get(i).getvValue();
                    break;
            }
        }
        for (int i = 0; i < nameI.size(); i ++) {
            switch (nameI.get(i).getvName()) {
                case "stepAvg":
                    stepAvg = nameI.get(i).getvValue();
                    break;
                case "stepEquil":
                    stepEquil = nameI.get(i).getvValue();
                    break;
                case "stepInitlzTemp":
                    stepInitlzTemp = nameI.get(i).getvValue();
                    break;
                case "stepLimit" :
                    stepLimit = nameI.get(i).getvValue();
                    break;
                case "limitTrajDev":
                    limitTrajDev = nameI.get(i).getvValue();
                    break;
                case "nebrTabFac":
                    nebrTabFac = nameI.get(i).getvValue();
                    break;
                case "stepTrajDev":
                    stepTrajDev = nameI.get(i).getvValue();
                    break;
                case "randSeed":
                    randSeed = nameI.get(i).getvValue();
                    break;
            }
        }
        System.out.println("==================================");
//  SetParams()
        rCut = Math.pow(2., 1./6.);
//        Region region = new Region(density, initUcell, "sc");
//        Region region = new Region(density, initUcell, "bcc");
//        Region region = new Region(density, initUcell, "fcc");
        Region region = new Region(density, initUcell, "diamond");
//        region.setnMol();
        nMol = region.nMol;
        System.out.println("structName = " + region.structName);
        System.out.printf("region = %f %f %f\n", region.x, region.y, region.z);
        velMag = Math.sqrt(NDIM * (1./nMol) * temperature); //        System.out.println("velMag = " + velMag);
        Cells cells = new Cells(rCut, rNebrShell, region);
        System.out.printf("cells = %d %d %d\n", cells.x, cells.y, cells.z);
        nebrTabMax = nebrTabFac * nMol;

//  SetUpJob(InitCoords)
        stepCount = 0;
        Coords coords = new Coords();

        System.out.printf("gap = %f %f %f\n", coords.gapX, coords.gapY, coords.gapZ);
        out1.printf("%s\nC\n", Integer.toString(nMol)); // jmol coords.d
//        System.out.println("Hello from coords!");
//        coords.setStructure();
        System.out.printf("coords = %f %f %f\n", coords.x, coords.y, coords.z);
        if (region.structName.equals("sc")) {
            coords.setGap(region, initUcell);
            int n = 0;
            for (int nz = 0; nz < initUcell.z; nz++) {
                for (int ny = 0; ny < initUcell.y; ny++) {
                    for (int nx = 0; nx < initUcell.x; nx++) {
                        coords.setInitCoords(nx + 0.5, ny + 0.5, nz + 0.5);
                        coords.setCoordsGap();
                        coords.setCoordsRegion(-0.5, region);
                        mol.add(new Mol());
                        mol.get(n).r.x = coords.x;
                        mol.get(n).r.z = coords.z;
                        mol.get(n).r.y = coords.y;
//                    System.out.printf("%s %f %f %f\n", 'C', mol.get(n).r.x, mol.get(n).r.y, mol.get(n).r.z);
                        out1.printf("%s %f %f %f\n", 'C', mol.get(n).r.x, mol.get(n).r.y, mol.get(n).r.z); // jmol
                        out2.printf("%f %f %f\n", mol.get(n).r.x, mol.get(n).r.y, mol.get(n).r.z); // gnuplot
                        n++;
                    }
                }
            }
        }
        else if (region.structName.equals("bcc")) {
            coords.setGap(region, initUcell);
            int n = 0;
            for (int nz = 0; nz < initUcell.z; nz++) {
                for (int ny = 0; ny < initUcell.y; ny++) {
                    for (int nx = 0; nx < initUcell.x; nx++) {
                        coords.setInitCoords(nx + 0.25, ny + 0.25, nz + 0.25);
                        coords.setCoordsGap();
                        coords.setCoordsRegion(-0.5, region);
                        for (int j = 0; j < 2; j ++) {
                            mol.add(new Mol());
                            mol.get(n).r.x = coords.x;
                            mol.get(n).r.z = coords.z;
                            mol.get(n).r.y = coords.y;
                            if (j == 1) {
                                mol.get(n).r.x += 0.5 + coords.gapX;
                                mol.get(n).r.z += 0.5 + coords.gapY;
                                mol.get(n).r.y += 0.5 + coords.gapZ;
                            }
                            out1.printf("%s %f %f %f\n", 'C', mol.get(n).r.x, mol.get(n).r.y, mol.get(n).r.z); // jmol
                            out2.printf("%f %f %f\n", mol.get(n).r.x, mol.get(n).r.y, mol.get(n).r.z); // gnuplot
                            n++;
                        }
                    }
                }
            }
        }
        else if (region.structName.equals("fcc")) {
            coords.setGap(region, initUcell);
            int n = 0;
            for (int nz = 0; nz < initUcell.z; nz++) {
                for (int ny = 0; ny < initUcell.y; ny++) {
                    for (int nx = 0; nx < initUcell.x; nx++) {
                        coords.setInitCoords(nx + 0.25, ny + 0.25, nz + 0.25);
                        coords.setCoordsGap();
                        coords.setCoordsRegion(-0.5, region);
                        for (int j = 0; j < 4; j ++) {
                            mol.add(new Mol());
                            mol.get(n).r.x = coords.x;
                            mol.get(n).r.z = coords.z;
                            mol.get(n).r.y = coords.y;
                            if (j != 3) {
                                if (j != 0)
                                    mol.get(n).r.x += 0.5 * coords.gapX;
                                if (j != 1)
                                    mol.get(n).r.y += 0.5 * coords.gapY;
                                if (j != 2)
                                    mol.get(n).r.z += 0.5 * coords.gapZ;
                            }
                            out1.printf("%s %f %f %f\n", 'C', mol.get(n).r.x, mol.get(n).r.y, mol.get(n).r.z); // jmol
                            out2.printf("%f %f %f\n", mol.get(n).r.x, mol.get(n).r.y, mol.get(n).r.z); // gnuplot
                            n++;
                        }
                    }
                }
            }
        }
        else if (region.structName.equals("diamond")) {
            double subShift;
            coords.setGap(region, initUcell);
            int n = 0;
            for (int nz = 0; nz < initUcell.z; nz++) {
                for (int ny = 0; ny < initUcell.y; ny++) {
                    for (int nx = 0; nx < initUcell.x; nx++) {
                        coords.setInitCoords(nx + 0.125, ny + 0.125, nz + 0.125);
                        coords.setCoordsGap();
                        coords.setCoordsRegion(-0.5, region);
                        for (int m = 0; m < 2; m ++) {
                            subShift = (m == 1) ? 0.25 : 0.;
                            for (int j = 0; j < 4; j ++) {
                                mol.add(new Mol());
                                mol.get(n).r.x = coords.x + subShift * coords.gapX;
                                mol.get(n).r.y = coords.y + subShift * coords.gapY;
                                mol.get(n).r.z = coords.z + subShift * coords.gapZ;
                                if (j != 3) {
                                    if (j != 0)
                                        mol.get(n).r.x += 0.5 * coords.gapX;
                                    if (j != 1)
                                        mol.get(n).r.y += 0.5 * coords.gapY;
                                    if (j != 2)
                                        mol.get(n).r.z += 0.5 * coords.gapZ;
                                }
                                out1.printf("%s %f %f %f\n", 'C', mol.get(n).r.x, mol.get(n).r.y, mol.get(n).r.z); // jmol
                                out2.printf("%f %f %f\n", mol.get(n).r.x, mol.get(n).r.y, mol.get(n).r.z); // gnuplot
                                n++;
                            }
                        }
                    }
                }
            }
        }
        else
            System.out.println("Error! Wrong Structure!");
//        SetUpJobs InitVels()
        InitVels initVels = new InitVels();
        veloSum.setZeroR();
        System.out.printf("nMol = %d\n", nMol);
        for (int i = 0; i < nMol; i ++) {
//            mol.add(new Mol());
            initVels.vRand();
            initVels.setVScale(velMag);
            mol.get(i).rv.x = initVels.x;
            mol.get(i).rv.y = initVels.y;
            mol.get(i).rv.z = initVels.z;
            out3.printf("%f %f %f\n", mol.get(i).rv.x, mol.get(i).rv.y, mol.get(i).rv.z); //velo.d
//            out3.printf("%d %f %f %f\n", i + 1, mol.get(i).rv.x, mol.get(i).rv.y, mol.get(i).rv.z);
            veloSum.setVAdd(mol.get(i).rv);
        }

        System.out.printf("%f %f %f\n", veloSum.x, veloSum.y, veloSum.z);
        for (int i = 0; i < mol.size(); i ++) {
            mol.get(i).setAvgVel(- 1. / nMol, veloSum);
            out4.printf("%f %f %f\n", mol.get(i).rv.x, mol.get(i).rv.y, mol.get(i).rv.z); //veloAvg.d
        }
//        InitAccels()
        for (int i = 0; i < mol.size(); i ++) {
            mol.get(i).setZeroRA();
//            System.out.printf("mol.ra = %f %f %f\n", mol.get(i).ra.x, mol.get(i).ra.y, mol.get(i).ra.z);
        }

// AccumProps()
        totEnergy.propZero();
        kinEnergy.propZero();
        pressure.propZero();
        kinEnInitSum = 0.;
        nebrNow = 1;
//============= End of SetUpJobs() =========================
        SetParams setParams = new SetParams();
        System.out.println("mol size = " + mol.size());
        moreCycles = true;
        while (moreCycles) {
            ++ stepCount;
            timeNow = stepCount * deltaT;
            for (int i = 0; i < mol.size(); i ++) {
//                mol.add(new Mol());
//                calcMet.leapFrogStep(0.5 * deltaT, mol.get(i).ra);
                setParams.VVSAdd(mol.get(i).rv, 0.5 * deltaT, mol.get(i).ra);
                System.out.printf("mol.rv = %f %f %f\n", mol.get(i).rv.x, mol.get(i).rv.y, mol.get(i).rv.z);
                setParams.VVSAdd(mol.get(i).r, deltaT, mol.get(i).rv);
//                mol.get(i).rv.x = calcMet.x;
//                mol.get(i).rv.y = calcMet.y;
//                mol.get(i).rv.z = calcMet.z;
//                calcMet.leapFrogStep(deltaT, mol.get(i).rv);
//                mol.get(i).r.x = calcMet.x;
//                mol.get(i).r.y = calcMet.y;
//                mol.get(i).r.z = calcMet.z;
//                System.out.printf("%d %f %f %f\t%f %f %f\n", i, mol.get(i).r.x, mol.get(i).r.y, mol.get(i).r.z,
//                        mol.get(i).rv.x, mol.get(i).rv.y, mol.get(i).rv.z);
                out5.printf("%s %f %f %f\n", 'C', mol.get(i).r.x, mol.get(i).r.y, mol.get(i).r.z); // jmol
                out6.printf("%f %f %f\n", mol.get(i).rv.x, mol.get(i).rv.y, mol.get(i).rv.z);
            }
            moreCycles = false;
        }
//        System.out.printf("totEnergy %f %f\n", totEnergy.sum, totEnergy.sum2);
//        System.out.printf("kinEnergy %f %f\n", kinEnergy.sum, kinEnergy.sum2);
//        System.out.printf("pressure %f %f\n", pressure.sum, pressure.sum2);
        out1.close(); //jmol coords.d
        out2.close(); //gnuplot gcoords.d
        out3.close(); //gnuplot velocityScale.d
        out4.close(); //gnuplot
        out5.close();
        out6.close();
        System.out.println("nMol = " + nMol);
    }
}