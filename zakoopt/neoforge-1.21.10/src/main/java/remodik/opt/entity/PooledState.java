package remodik.opt.entity;

public interface PooledState {
	Object zakoopt$state();

	Object zakoopt$stateOwner();

	void zakoopt$setState(Object state, Object owner);
}
