package net.himeki.btrback;

import net.himeki.btrback.tasks.RollbackTask;

public class Rollback {
    public static void main(String[] args) {                //For rollback process
        if (!RollbackTask.doRollbackStageTwo())
            System.err.println("Rollback failed.");
        else {
            System.out.println("Stage two done.");
        }
    }
}
