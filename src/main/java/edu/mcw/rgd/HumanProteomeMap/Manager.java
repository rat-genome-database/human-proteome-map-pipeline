package edu.mcw.rgd.HumanProteomeMap;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

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

    Logger log = Logger.getLogger("core");
    Logger logStatus = Logger.getLogger("status");

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        Manager manager = (Manager) (bf.getBean("manager"));

        try {
            manager.run();
        }catch (Exception e) {
            manager.log.error(e);
            manager.logStatus.error(e);
            throw e;
        }
    }

    public void run() throws Exception {
        run(SpeciesType.HUMAN);
    }

    public void run(int speciesTypeKey) throws Exception {

        long startTime = System.currentTimeMillis();

        String msg = getVersion();
        log.info(msg);
        logStatus.info(msg);

        msg = dao.getConnectionInfo();
        log.info(msg);
        logStatus.info(msg);

        String species = SpeciesType.getCommonName(speciesTypeKey);
        msg = "START: " + getPipelineName() + " ID generation starting for " + species;
        log.info(msg);
        logStatus.info(msg);

        // QC
        log.info("QC: get "+getPipelineName()+" Ids in RGD for "+species);
        List<XdbId> idsInRgd = dao.getHumanProteomeMapIds(speciesTypeKey, getPipelineName());
        log.info("QC: get incoming "+getPipelineName()+" Ids for "+species);
        List<XdbId> idsIncoming = getIncomingIds(speciesTypeKey);

        // determine to-be-inserted Human Proteome Map ids
        log.info("QC: determine to-be-inserted "+getPipelineName()+" Ids");
        List<XdbId> idsToBeInserted = new ArrayList<XdbId>(idsIncoming);
        idsToBeInserted.removeAll(idsInRgd);

        // determine matching Human Proteome Map ids
        log.info("QC: determine matching "+getPipelineName()+" Ids");
        List<XdbId> idsMatching = new ArrayList<XdbId>(idsIncoming);
        idsMatching.retainAll(idsInRgd);

        // determine to-be-deleted Human Proteome Map ids
        log.info("QC: determine to-be-deleted "+getPipelineName()+" Ids");
        idsInRgd.removeAll(idsIncoming);
        List<XdbId> idsToBeDeleted = idsInRgd;


        // loading
        if( !idsToBeInserted.isEmpty() ) {
            msg = "  inserting "+getPipelineName()+" ids for "+species+": "+idsToBeInserted.size();
            log.info(msg);
            logStatus.info(msg);
            dao.insertXdbs(idsToBeInserted);
        }

        if( !idsToBeDeleted.isEmpty() ) {
            msg = "  deleting "+getPipelineName()+" ids for "+species+": "+idsToBeDeleted.size();
            log.info(msg);
            logStatus.info(msg);
            dao.deleteXdbIds(idsToBeDeleted);
        }

        if( !idsMatching.isEmpty() ) {
            msg = "  matching "+getPipelineName()+" ids for "+species+": "+idsMatching.size();
            log.info(msg);
            logStatus.info(msg);
            dao.updateModificationDate(idsMatching);
        }

        msg = "END: "+getPipelineName() + " ID generation complete for " + species;
        log.info(msg);
        logStatus.info(msg);

        msg = "===    time elapsed: "+ Utils.formatElapsedTime(startTime, System.currentTimeMillis());
        log.info(msg);
        logStatus.info(msg);
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
}

