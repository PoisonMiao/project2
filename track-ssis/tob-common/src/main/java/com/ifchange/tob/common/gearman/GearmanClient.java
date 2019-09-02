package com.ifchange.tob.common.gearman;

import com.ifchange.tob.common.gearman.lib.Gearman;
import com.ifchange.tob.common.gearman.lib.GearmanProducer;
import com.ifchange.tob.common.gearman.lib.GearmanServer;

public final class GearmanClient {
    private static GearmanProducer client;

    GearmanClient(Gearman gearman) {
        GearmanClient.client = gearman.createGearmanClient();
        GearmanServer server = gearman.createGearmanServer();
        GearmanClient.client.registryServer(server);
    }
/*
    public static void async(String function, Object request) {
        GearmanJobReturn jobReturn = client.submitBackgroundJob(function, JsonHelper.toJSONBytes(request));
        while (!jobReturn.isEOF()) {
            // Poll the next job event (blocking operation)
            GearmanJobEvent event = jobReturn.poll();
            switch (event.getEventType()) {
                // success
                case GEARMAN_JOB_SUCCESS: // Job completed successfully
                    // print the result
                    System.out.println(new String(event.getData()));
                    break;
                // failure
                case GEARMAN_SUBMIT_FAIL: // The job submit operation failed
                case GEARMAN_JOB_FAIL: // The job's execution failed
                    System.err.println(event.getEventType() + ": " + new String(event.getData()));

            }
        }
    }
*/
}
