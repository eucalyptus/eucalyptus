package com.eucalyptus.reporting.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import com.eucalyptus.event.Event;
import com.eucalyptus.util.OwnerFullName;

@SuppressWarnings("serial")
public class SnapShotEvent implements Event {

  public enum SnapShotAction {
    SNAPSHOTCREATE,
    SNAPSHOTDELETE
  }

  public static class CreateActionInfo extends EventActionInfo<SnapShotAction> {
    private final Long size;

    private CreateActionInfo( final Long size ) {
      super( SnapShotAction.SNAPSHOTCREATE );
      this.size = size;
    }

    /**
     * Get the size in GiB
     */
    public Long getSize() {
      return size;
    }

    public String toString() {
      return String.format("[action:%s,size:%s]", getAction(), getSize());
    }
  }

  private final EventActionInfo<SnapShotAction> actionInfo;
  private final OwnerFullName ownerFullName;
  private final String snapshotId;
  private final String uuid;

  /**
   * Action for snapshot creation.
   *
   * @param size The snapshot size in GiB
   * @return The action info
   */
  public static EventActionInfo<SnapShotAction> forSnapShotCreate( final Long size ) {
    assertThat(size, notNullValue());

    return new CreateActionInfo( size );
  }

  public static EventActionInfo<SnapShotAction> forSnapShotDelete() {
    return new EventActionInfo<SnapShotAction>(SnapShotAction.SNAPSHOTDELETE);
  }

  public static SnapShotEvent with( final EventActionInfo<SnapShotAction> actionInfo,
                                    final String snapShotUUID,
                                    final String snapshotId,
                                    final OwnerFullName ownerFullName ) {

    return new SnapShotEvent( actionInfo, snapShotUUID, snapshotId, ownerFullName );
  }

  private SnapShotEvent( final EventActionInfo<SnapShotAction> actionInfo,
                         final String uuid,
                         final String snapshotId,
                         final OwnerFullName ownerFullName ) {
    assertThat(actionInfo, notNullValue());
    assertThat(uuid, notNullValue());
    assertThat(ownerFullName.getUserId(), notNullValue());
    assertThat(snapshotId, notNullValue());
    this.actionInfo = actionInfo;
    this.ownerFullName = ownerFullName;
    this.snapshotId = snapshotId;
    this.uuid = uuid;
  }

  public String getSnapshotId() {
    return snapshotId;
  }

  public OwnerFullName getOwner() {
    return ownerFullName;
  }

  public EventActionInfo<SnapShotAction> getActionInfo() {
    return actionInfo;
  }

  public String getUuid() {
    return uuid;
  }

  @Override
  public String toString() {
    return "SnapShotEvent [actionInfo=" + actionInfo
        + ", userName=" + ownerFullName.getUserName() + ", snapshotId="
        + snapshotId + ", uuid=" + uuid + "]";
  }
}
