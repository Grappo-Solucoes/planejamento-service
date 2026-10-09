package br.com.busco.planejamento.domain;

/**
 * Máquina de estados do planejamento, com as transições exatas do monólito
 * ({@code br.com.grappo.bus.core.planejamento.domain.Status}).
 */
public enum Status {
    PLANEJADA {
        @Override
        public Status iniciar() {
            return INICIADA;
        }

        @Override
        public Status finalizar() {
            return this;
        }

        @Override
        public Status cancelar() {
            return CANCELADA;
        }
    },
    INICIADA {
        @Override
        public Status iniciar() {
            return this;
        }

        @Override
        public Status finalizar() {
            return FINALIZADA;
        }

        @Override
        public Status cancelar() {
            return this;
        }
    },
    FINALIZADA {
        @Override
        public Status iniciar() {
            return this;
        }

        @Override
        public Status finalizar() {
            return this;
        }

        @Override
        public Status cancelar() {
            return this;
        }
    },
    CANCELADA {
        @Override
        public Status iniciar() {
            return this;
        }

        @Override
        public Status finalizar() {
            return this;
        }

        @Override
        public Status cancelar() {
            return this;
        }
    },
    EM_ANALISE {
        @Override
        public Status planejar() {
            return PLANEJADA;
        }

        @Override
        public Status cancelar() {
            return CANCELADA;
        }
    };

    public Status iniciar() {
        throw new UnsupportedOperationException("Ação não suportada para o estado atual.");
    }

    public Status finalizar() {
        throw new UnsupportedOperationException("Ação não suportada para o estado atual.");
    }

    public Status cancelar() {
        throw new UnsupportedOperationException("Ação não suportada para o estado atual.");
    }

    public Status planejar() {
        throw new UnsupportedOperationException("Ação não suportada para o estado atual.");
    }
}
