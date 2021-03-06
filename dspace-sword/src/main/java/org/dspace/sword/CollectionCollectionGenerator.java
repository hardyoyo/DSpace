/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.purl.sword.base.Collection;

/**
 * Class to generate ATOM Collection Elements which represent
 * DSpace Collections
 */
public class CollectionCollectionGenerator extends ATOMCollectionGenerator {
    /**
     * logger
     */
    private static final Logger log =
        LogManager.getLogger(CollectionCollectionGenerator.class);

    protected CollectionService collectionService =
        ContentServiceFactory.getInstance().getCollectionService();

    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Construct an object taking the SWORD service instance an argument
     *
     * @param service SWORD service implementation
     */
    public CollectionCollectionGenerator(SWORDService service) {
        super(service);
        log.debug("Create new instance of CollectionCollectionGenerator");
    }

    /**
     * Build the collection for the given DSpaceObject.  In this implementation,
     * if the object is not a DSpace Collection, it will throw an exception.
     *
     * @param dso target DSpace object
     * @throws DSpaceSWORDException can be thrown by the internals of the DSpace SWORD implementation
     */
    @Override
    public Collection buildCollection(DSpaceObject dso)
        throws DSpaceSWORDException {
        if (!(dso instanceof org.dspace.content.Collection)) {
            log.error(
                "buildCollection passed argument which is not of type Collection");
            throw new DSpaceSWORDException(
                "Incorrect ATOMCollectionGenerator instantiated");
        }

        // get the things we need out of the service
        SWORDConfiguration swordConfig = swordService.getSwordConfig();
        SWORDUrlManager urlManager = swordService.getUrlManager();

        org.dspace.content.Collection col = (org.dspace.content.Collection) dso;

        Collection scol = new Collection();

        // prepare the parameters to be put in the sword collection
        String location = urlManager.getDepositLocation(col);

        // collection title is just its name
        String title = collectionService.getMetadataFirstValue(col,
                CollectionService.MD_NAME, Item.ANY);

        // the collection policy is the licence to which the collection adheres
        String collectionPolicy = collectionService.getLicense(col);

        // FIXME: what is the treatment?  Doesn't seem appropriate for DSpace
        // String treatment = " ";

        // abstract is the short description of the collection
        String dcAbstract = collectionService.getMetadataFirstValue(col,
                CollectionService.MD_SHORT_DESCRIPTION, Item.ANY);

        // we just do support mediation
        boolean mediation = swordConfig.isMediated();

        // load up the sword collection
        scol.setLocation(location);

        // add the title if it exists
        if (title != null && !"".equals(title)) {
            scol.setTitle(title);
        }

        // add the collection policy if it exists
        if (collectionPolicy != null && !"".equals(collectionPolicy)) {
            scol.setCollectionPolicy(collectionPolicy);
        }

        // FIXME: leave the treatment out for the time being,
        // as there is no analogue
        // scol.setTreatment(treatment);

        // add the abstract if it exists
        if (dcAbstract != null && !"".equals(dcAbstract)) {
            scol.setAbstract(dcAbstract);
        }

        scol.setMediation(mediation);

        List<String> accepts = swordService.getSwordConfig()
                                           .getCollectionAccepts();
        for (String accept : accepts) {
            scol.addAccepts(accept);
        }

        // add the accept packaging values
        Map<String, Float> aps = swordConfig.getAcceptPackaging(col);
        for (Map.Entry<String, Float> ap : aps.entrySet()) {
            scol.addAcceptPackaging(ap.getKey(), ap.getValue());
        }

        // should we offer the items in the collection up as deposit
        // targets?
        boolean itemService = configurationService
            .getBooleanProperty("sword-server.expose-items");
        if (itemService) {
            String subService = urlManager.constructSubServiceUrl(col);
            scol.setService(subService);
        }

        log.debug("Created ATOM Collection for DSpace Collection");

        return scol;
    }
}
