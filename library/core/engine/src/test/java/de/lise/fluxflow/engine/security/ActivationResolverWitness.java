package de.lise.fluxflow.engine.security;

/** Harmless fixtures that record class initialization and construction. */
public final class ActivationResolverWitness {
    private ActivationResolverWitness() {
    }

    public static final class StepOtherRoleWitness {
        static {
            System.setProperty(StepOtherRoleWitness.class.getName() + ".initialized", "true");
        }

        public StepOtherRoleWitness() {
            System.setProperty(StepOtherRoleWitness.class.getName() + ".constructed", "true");
        }
    }

    public static final class StepLegacyWitness {
        static {
            System.setProperty(StepLegacyWitness.class.getName() + ".initialized", "true");
        }

        public StepLegacyWitness() {
            System.setProperty(StepLegacyWitness.class.getName() + ".constructed", "true");
        }
    }

    public static final class JobOtherRoleWitness {
        static {
            System.setProperty(JobOtherRoleWitness.class.getName() + ".initialized", "true");
        }

        public JobOtherRoleWitness() {
            System.setProperty(JobOtherRoleWitness.class.getName() + ".constructed", "true");
        }

        public void execute() {
            // Valid job payload, deliberately never invoked by the rejection test.
        }
    }

    public static final class JobLegacyWitness {
        static {
            System.setProperty(JobLegacyWitness.class.getName() + ".initialized", "true");
        }

        public JobLegacyWitness() {
            System.setProperty(JobLegacyWitness.class.getName() + ".constructed", "true");
        }

        public void execute() {
            // Valid job payload, deliberately never invoked by the rejection test.
        }
    }
}
