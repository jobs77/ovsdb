/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.ready.SystemReadyListener;
import org.opendaylight.infrautils.ready.SystemReadyMonitor;
import org.opendaylight.infrautils.ready.SystemState;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.eos.binding.api.Entity;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipChange;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipListener;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipChangeState;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class SouthboundProviderTest extends AbstractDataBrokerTest {

    private EntityOwnershipService entityOwnershipService;

    @Before
    public void setUp() throws CandidateAlreadyRegisteredException {
        entityOwnershipService = mock(EntityOwnershipService.class);
        when(entityOwnershipService.registerListener(anyString(), any(EntityOwnershipListener.class))).thenReturn(
                mock(EntityOwnershipListenerRegistration.class));
        when(entityOwnershipService.registerCandidate(any(Entity.class))).thenReturn(mock(
                EntityOwnershipCandidateRegistration.class));
    }

    @Test
    public void testInit() throws CandidateAlreadyRegisteredException {
        // Indicate that this is the owner
        when(entityOwnershipService.getOwnershipState(any(Entity.class))).thenReturn(
                Optional.of(EntityOwnershipState.from(true, true)));

        try (SouthboundProvider southboundProvider = new SouthboundProvider(
                getDataBroker(),
                entityOwnershipService,
                Mockito.mock(OvsdbConnection.class),
                Mockito.mock(DOMSchemaService.class),
                Mockito.mock(BindingNormalizedNodeSerializer.class),
                new ImmediateSystemReadyMonitor(),
                Mockito.mock(DiagStatusService.class))) {

            // Initiate the session
            southboundProvider.init();

            // Verify that at least one listener was registered
            verify(entityOwnershipService, atLeastOnce()).registerListener(
                    anyString(), any(EntityOwnershipListener.class));

            // Verify that a candidate was registered
            verify(entityOwnershipService).registerCandidate(any(Entity.class));
        }
    }

    @Test
    public void testInitWithClose() throws CandidateAlreadyRegisteredException {
        // Indicate that this is the owner
        when(entityOwnershipService.getOwnershipState(any(Entity.class))).thenReturn(
                Optional.of(EntityOwnershipState.from(true, true)));

        try (SouthboundProvider southboundProvider = new SouthboundProvider(
                getDataBroker(),
                entityOwnershipService,
                Mockito.mock(OvsdbConnection.class),
                Mockito.mock(DOMSchemaService.class),
                Mockito.mock(BindingNormalizedNodeSerializer.class),
                new ImmediateSystemReadyMonitor(),
                Mockito.mock(DiagStatusService.class))) {

            // Initiate the session
            southboundProvider.init();

            // Verify that at least one listener was registered
            verify(entityOwnershipService, atLeastOnce()).registerListener(
                    anyString(), any(EntityOwnershipListener.class));

            // Verify that a candidate was registered
            verify(entityOwnershipService).registerCandidate(any(Entity.class));

            //Close the session
            southboundProvider.close();
        }
    }

    @Test
    public void testGetDb() {
        when(entityOwnershipService.getOwnershipState(any(Entity.class))).thenReturn(
                Optional.of(EntityOwnershipState.from(true, true)));

        try (SouthboundProvider southboundProvider = new SouthboundProvider(
                getDataBroker(),
                entityOwnershipService,
                Mockito.mock(OvsdbConnection.class),
                Mockito.mock(DOMSchemaService.class),
                Mockito.mock(BindingNormalizedNodeSerializer.class),
                new ImmediateSystemReadyMonitor(),
                Mockito.mock(DiagStatusService.class))) {

            southboundProvider.init();

            assertEquals(getDataBroker(), SouthboundProvider.getDb());
        }
    }

    @Test
    public void testHandleOwnershipChange() throws ReadFailedException {
        when(entityOwnershipService.getOwnershipState(any(Entity.class))).thenReturn(
                Optional.of(EntityOwnershipState.from(false, true)));
        Entity entity = new Entity("ovsdb-southbound-provider", "ovsdb-southbound-provider");
        KeyedInstanceIdentifier<Topology, TopologyKey> topologyIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));

        try (SouthboundProvider southboundProvider = new SouthboundProvider(
                getDataBroker(),
                entityOwnershipService,
                Mockito.mock(OvsdbConnection.class),
                Mockito.mock(DOMSchemaService.class),
                Mockito.mock(BindingNormalizedNodeSerializer.class),
                new ImmediateSystemReadyMonitor(),
                Mockito.mock(DiagStatusService.class))) {

            southboundProvider.init();

            // At this point the OVSDB topology must not be present in either tree
            assertFalse(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                    topologyIid).checkedGet().isPresent());
            assertFalse(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL,
                    topologyIid).checkedGet().isPresent());

            // Become owner
            southboundProvider.handleOwnershipChange(new EntityOwnershipChange(entity,
                    EntityOwnershipChangeState.from(false, true, true)));

            // Now the OVSDB topology must be present in both trees
            assertTrue(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                    topologyIid).checkedGet().isPresent());
            assertTrue(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL,
                    topologyIid).checkedGet().isPresent());

            // Verify idempotency
            southboundProvider.handleOwnershipChange(new EntityOwnershipChange(entity,
                    EntityOwnershipChangeState.from(false, true, true)));

            // The OVSDB topology must be present in both trees
            assertTrue(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                    topologyIid).checkedGet().isPresent());
            assertTrue(getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL,
                    topologyIid).checkedGet().isPresent());
        }
    }

    private static class ImmediateSystemReadyMonitor implements SystemReadyMonitor {

        @Override
        public SystemState getSystemState() {
            return SystemState.ACTIVE;
        }

        @Override
        public void registerListener(SystemReadyListener listener) {
            listener.onSystemBootReady();
        }

        @Override
        public String getFailureCause() {
            return "";
        }
    }
}
