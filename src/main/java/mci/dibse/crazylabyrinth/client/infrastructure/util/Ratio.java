package mci.dibse.crazylabyrinth.client.infrastructure.util;

public enum Ratio {

        FOUR_THREE(0),
        SIXTEEN_NINE(1);

        private int id;

        /**
         * Return the ratio identifier.
         *
         * @return ratio id
         */
        public int getId() {
            return id;
        }

        Ratio(int id) {
            this.id = id;
        }
}
