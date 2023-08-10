/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.services.model.Event;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serialize {@link UsageWorkflowEvent} data to a file as Tab delimited. In dspace.cfg
 * specify the path to the file as the value of
 * {@code usageEvent.tabFileLogger.file}.  If that path is not absolute, it
 * will be interpreted as relative to the directory named in {@code log.dir}.
 * If no name is configured, it defaults to "usage-workflow-events.tsv".  If the file is
 * new or empty, a column heading record will be written when the file is opened.
 *
 * @author Mark H. Wood
 * @author Mark Diggory
 * @author Hardy Pottinger
 */
public class TabFileUsageWorkflowEventListener
    extends AbstractUsageEventListener {
    /**
     * log category.
     */
    private static final Logger errorLog = LoggerFactory
        .getLogger(TabFileUsageWorkflowEventListener.class);

    /**
     * ISO 8601 Basic string format for record timestamps.
     */
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
        "yyyyMMdd'T'HHmmssSSS");

    /**
     * File on which to write event records.
     */
    private PrintWriter eventLog;

    /**
     * Is this instance initialized?
     */
    private boolean initialized = false;

    /**
     * Set up a usage event listener for writing TSV records to a file.
     */
    private void init() {
        ConfigurationService configurationService
            = new DSpace().getConfigurationService();

        String logPath = configurationService.getPropertyAsType(
            "usageEvent.tabFileLogger.file",
            "usage-workflow-events.tsv");

        String logDir = null;
        if (!new File(logPath).isAbsolute()) {
            logDir = configurationService.getProperty("log.report.dir");
        }

        File logFile = new File(logDir, logPath);
        try {
            eventLog = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(logFile, true)));
            errorLog.debug("Writing to {}", logFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            errorLog.error("{} cannot open file, will not log events:  {}",
                           TabFileUsageWorkflowEventListener.class.getName(),
                           e.getMessage());
            throw new IllegalArgumentException("Cannot open event log file", e);
        }

        // TODO: ensure we are logging the fields we need to log to be useful
        //       remember that this is just a header, you'll also need to change
        //       the code that writes the records.
        //       some general suggestions: date, name of workflow step, eperson,
        //       role, outcome, comments, handle, item title
        //       not sure where date is from, but here are the fields in a
        //       UsageWorkflowEvent object:
        //              private String workflowStep;
        //              private String oldState;
        //              private EPerson[] epersonOwners;
        //              private Group[] groupOwners;
        //              private Collection scope;
        //              private EPerson actor;
        //              private InProgressSubmission workflowItem;
        //       NOTE: these are all private fields, so you'll need to use the
        //       associated getters to get the values
        if (logFile.length() <= 0) {
            eventLog.println("date"
                                 + '\t' + "workflowStep"
                                 + '\t' + "oldState"
                                 + '\t' + "epersonOwners"
                                 + '\t' + "groupOwners"
                                 + '\t' + "scope"
                                 + '\t' + "actor"
                                 + '\t' + "workflowItem");
        }

        initialized = true;
    }

    @Override
    public synchronized void receiveEvent(Event event) {
        if (!initialized) {
            init();
        }

        if (errorLog.isDebugEnabled()) {
            errorLog.debug("got: {}", event.toString());
        }

        // Only UsageWorkflowEvent events are logged
        if (!(event instanceof UsageWorkflowEvent)) {
            return;
        }

        if (null == eventLog) {
            return;
        }

        UsageWorkflowEvent ue = (UsageWorkflowEvent) event;

        eventLog.append(dateFormat.format(new Date()))
                .append('\t').append(ue.getName()) // event type
                .append('\t').append(Constants.typeText[ue.getObject().getType()])
                .append('\t').append(ue.getObject().getID().toString())
                .append('\t').append(ue.getRequest().getSession().getId())
                .append('\t').append(ue.getRequest().getRemoteAddr());

        String epersonName = (null == ue.getContext().getCurrentUser()
            ? "anonymous"
            : ue.getContext().getCurrentUser().getEmail());
        eventLog.append('\t').append(epersonName);

        eventLog.println();
        eventLog.flush();
    }
}
