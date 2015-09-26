package org.jaywixz;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.jaywixz.archive.ArchiveFile;


public final class Jaywixz {

    private static final Logger log = Logger.getLogger(Jaywixz.class);

    public static void main(String[] args) throws Exception {
        if(args.length != 1) {
            System.out.println("usage: " + Jaywixz.class.getName() + " <archive.txz>");
            return;
        }
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        log.info("starting..");
        long start = System.currentTimeMillis();
        ArchiveFile archive = new ArchiveFile(args[0]);
        log.info("start " + ((System.currentTimeMillis() - start) / 1000) + "s " + archive.toString());
        new WebServer(archive).start();
    }
}
