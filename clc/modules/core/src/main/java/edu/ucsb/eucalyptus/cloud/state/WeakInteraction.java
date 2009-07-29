package edu.ucsb.eucalyptus.cloud.state;

import java.lang.ref.WeakReference;

public class WeakInteraction<REF, MEMENTO> {
  private WeakReference<REF> reference;
  private String referenceKey;
  private State boundState;
  private MEMENTO memento;

  public WeakInteraction() {
    this.boundState = State.GENERATING;
  }

  public void generate( final WeakReference<REF> reference, final MEMENTO memento ) {
    this.reference = reference;
    this.memento = memento;
    this.boundState = State.GENERATING;
  }

  public WeakReference<REF> getReference() {
    return reference;
  }

  private void setReference( final WeakReference<REF> reference ) {
    this.reference = reference;
  }

  public State getBoundState() {
    return boundState;
  }

  private void setBoundState( final State boundState ) {
    this.boundState = boundState;
  }

  public MEMENTO getMemento() {
    return memento;
  }

  private void setMemento( final MEMENTO memento ) {
    this.memento = memento;
  }
}
