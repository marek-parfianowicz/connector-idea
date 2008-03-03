package com.atlassian.theplugin.idea.crucible;

import com.atlassian.theplugin.ServerType;
import com.atlassian.theplugin.bamboo.MissingPasswordHandler;
import com.atlassian.theplugin.configuration.ConfigurationFactory;
import com.atlassian.theplugin.configuration.Server;
import com.atlassian.theplugin.configuration.ServerPasswordNotProvidedException;
import com.atlassian.theplugin.crucible.CrucibleServerFactory;
import com.atlassian.theplugin.crucible.CrucibleStatusListener;
import com.atlassian.theplugin.crucible.ReviewDataInfo;
import com.atlassian.theplugin.crucible.api.CrucibleLoginException;
import com.atlassian.theplugin.idea.SchedulableComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;


/**
 * IDEA-specific class that uses {@link com.atlassian.theplugin.crucible.CrucibleServerFactory} to retrieve builds info and
 * passes raw data to configured {@link com.atlassian.theplugin.crucible.CrucibleStatusListener}s.<p>
 * <p/>
 * Intended to be triggered by a {@link java.util.Timer} through the {@link #newTimerTask()}.<p>
 * <p/>
 * Thread safe.
 */
public final class CrucibleStatusChecker implements SchedulableComponent {
	private static final long CRUCIBLE_TIMER_TICK = 120000;
	private static final Logger LOGGER = Logger.getInstance("#com.atlassian.theplugin.idea.PluginStatusBarToolTip");
	//private static final Category LOGGER = Logger.getInstance(PluginStatusBarToolTip.class);
	private final List<CrucibleStatusListener> listenerList = new ArrayList<CrucibleStatusListener>();
	
	public void registerListener(CrucibleStatusListener listener) {
		synchronized (listenerList) {
			listenerList.add(listener);
		}
	}

	public void unregisterListener(CrucibleStatusListener listener) {
		synchronized (listenerList) {
			listenerList.remove(listener);
		}
	}

	/**
	 * DO NOT use that method in 'dispatching thread' of IDEA. It can block GUI for several seconds.
	 */
	private void doRun() {
        try {
            // collect build info from each server
            final Collection<ReviewDataInfo> reviews = new ArrayList<ReviewDataInfo>();
            for (Server server : retrieveEnabledCrucibleServers()) {
                                try {
                                    reviews.addAll(
                                            CrucibleServerFactory.getCrucibleServerFacade().getActiveReviewsForUser(server));
                                } catch (ServerPasswordNotProvidedException exception) {
                                    ApplicationManager.getApplication().invokeLater(
                                            new MissingPasswordHandler(), ModalityState.defaultModalityState());
                                } catch (CrucibleLoginException e) {
									LOGGER.error("Error getting Crucible reviews for " + server.getName() + " server", e);
                                }
                            }

            // dispatch to the listeners
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    synchronized (listenerList) {
                        for (CrucibleStatusListener listener : listenerList) {
                            listener.updateReviews(reviews);
                        }
                    }
                }
            });
	    } catch (Throwable t) {
			t.printStackTrace();
        }
    }

	private static Collection<Server> retrieveEnabledCrucibleServers() {
		return ConfigurationFactory.getConfiguration().getProductServers(
                            ServerType.CRUCIBLE_SERVER).getEnabledServers();
	}

	/**
	 * Create a new instance of {@link java.util.TimerTask} for {@link java.util.Timer} re-scheduling purposes.
	 *
	 * @return new instance of TimerTask
	 */
	public TimerTask newTimerTask() {
        return new TimerTask() {
            public void run() {
                doRun();
            }
        };
	}

	public boolean canSchedule() {
		return !retrieveEnabledCrucibleServers().isEmpty();
	}

	public long getInterval() {
		return CRUCIBLE_TIMER_TICK;
	}
}