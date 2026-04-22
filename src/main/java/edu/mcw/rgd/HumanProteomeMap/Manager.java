package edu.mcw.rgd.HumanProteomeMap;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.process.MemoryMonitor;
import edu.mcw.rgd.process.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author jdepons
 * @since 4/26/12
 */
public class Manager {

    private DAO dao = new DAO();
    private String version;
    private String pipelineName;
    private String staleXdbDeleteThreshold;

    Logger log = LogManager.getLogger("status");

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        Manager manager = (Manager) (bf.getBean("manager"));

        try {
            manager.run();
        }catch (Exception e) {
            Utils.printStackTrace(e, manager.log);
            throw e;
        }
    }

    public void run() throws Exception {
        run(SpeciesType.HUMAN);
    }

    public void run(int speciesTypeKey) throws Exception {

        long startTime = System.currentTimeMillis();

        MemoryMonitor memoryMonitor = new MemoryMonitor();
        memoryMonitor.start();

        String msg = getVersion();
        log.info(msg);

        msg = dao.getConnectionInfo();
        log.info("   "+msg);

        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("   started at "+sdt.format(new Date(startTime)));
        log.info("");

        String species = SpeciesType.getCommonName(speciesTypeKey);
        log.info("Update "+getPipelineName()+" external database ids for "+species);

        // QC
        log.debug("QC: get "+getPipelineName()+" Ids in RGD");
        List<XdbId> idsInRgd = dao.getHumanProteomeMapIds(speciesTypeKey, getPipelineName());
        int originalCount = idsInRgd.size();
        log.debug("QC: get incoming "+getPipelineName()+" Ids");
        List<XdbId> idsIncoming = getIncomingIds(speciesTypeKey);

        // determine to-be-inserted Human Proteome Map ids
        log.debug("QC: determine to-be-inserted "+getPipelineName()+" Ids");
        List<XdbId> idsToBeInserted = new ArrayList<>(CollectionUtils.subtract(idsIncoming, idsInRgd));

        // determine matching Human Proteome Map ids
        log.debug("QC: determine matching "+getPipelineName()+" Ids");
        List<XdbId> idsMatching = new ArrayList<>(CollectionUtils.intersection(idsIncoming, idsInRgd));

        // determine to-be-deleted Human Proteome Map ids
        log.debug("QC: determine to-be-deleted "+getPipelineName()+" Ids");
        List<XdbId> idsToBeDeleted = new ArrayList<>(CollectionUtils.subtract(idsInRgd, idsIncoming));

        // loading
        if( !idsToBeInserted.isEmpty() ) {
            dao.insertXdbs(idsToBeInserted);
            msg = "  inserted ids : "+Utils.formatThousands(idsToBeInserted.size());
            log.info(msg);
        }

        int deleted = 0;
        if( !idsToBeDeleted.isEmpty() ) {
            // convert delete-threshold string to number; i.e. '5%' --> '5'
            int threshPerc = Integer.parseInt(staleXdbDeleteThreshold.substring(0, staleXdbDeleteThreshold.length()-1));
            int deleteLimit = (threshPerc * originalCount) / 100;
            log.info("  stale xdb ids delete limit ("+staleXdbDeleteThreshold+"): "+Utils.formatThousands(deleteLimit));
            log.info("  stale xdb ids to be deleted: "+Utils.formatThousands(idsToBeDeleted.size()));

            if( idsToBeDeleted.size() > deleteLimit ) {
                log.warn("*** DELETE of stale xdb ids aborted! *** "+staleXdbDeleteThreshold+" delete threshold exceeded!");
            } else {
                deleted = dao.deleteXdbIds(idsToBeDeleted);
                log.info("  deleted ids : "+Utils.formatThousands(deleted));
            }
        }

        if( !idsMatching.isEmpty() ) {
            dao.updateModificationDate(idsMatching);
            msg = "  matching ids : "+Utils.formatThousands(idsMatching.size());
            log.info(msg);
        }

        int countAdj = idsToBeInserted.size() - deleted;
        int newCount = originalCount + countAdj;
        msg = String.format("new total of %s ids: %s (change: %s)", getPipelineName(), Utils.formatThousands(newCount), Utils.formatThousands(countAdj));
        log.info(msg);

        memoryMonitor.stop();
        log.info(memoryMonitor.getSummary());

        msg = "=== OK ===  time elapsed: "+ Utils.formatElapsedTime(startTime, System.currentTimeMillis());
        log.info(msg);

        log.info("");
    }

    List<XdbId> getIncomingIds(int speciesTypeKey) throws Exception {

        List<Gene> genes = dao.getActiveGenes(speciesTypeKey);
        List<XdbId> incomingIds = new ArrayList<XdbId>(genes.size());
        for (Gene g: genes) {
            XdbId x = new XdbId();
            x.setAccId(g.getSymbol());
            x.setSrcPipeline(getPipelineName());
            x.setRgdId(g.getRgdId());
            x.setXdbKey(56);
            x.setCreationDate(new Date());
            x.setModificationDate(new Date());
            incomingIds.add(x);
        }
        return incomingIds;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public void setStaleXdbDeleteThreshold(String staleXdbDeleteThreshold) {
        this.staleXdbDeleteThreshold = staleXdbDeleteThreshold;
    }

    public String getStaleXdbDeleteThreshold() {
        return staleXdbDeleteThreshold;
    }
}

