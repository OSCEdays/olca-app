/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.layout.GraphLayoutType;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProcessPart;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.editors.graphical.model.TreeConnectionRouter;
import org.openlca.app.editors.graphical.outline.OutlinePage;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProductSystemGraphEditor extends GraphicalEditor implements
		PropertyChangeListener {

	public static final String ID = "editors.productsystem.graphical";

	private ProductSystem productSystem;
	private ProductSystemNode model;
	private GraphLayoutType layoutType = GraphLayoutType.TREE_LAYOUT;
	private OutlinePage outline;
	private boolean routed;
	private GraphicalViewerConfigurator configurator;

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		if (input instanceof GraphicalEditorInput) {
			GraphicalEditorInput modelInput = (GraphicalEditorInput) input;
			productSystem = new ProductSystemDao(Database.get())
					.getForId(modelInput.getDescriptor().getId());
			if (productSystem != null)
				setPartName(productSystem.getName());
		}
		super.init(site, input);
	}

	private ProductSystemNode createModel() {
		Set<Long> processes = new HashSet<>();
		long referenceId = productSystem.getReferenceProcess().getId();
		processes.add(referenceId);
		for (ProcessLink link : productSystem.getIncomingLinks(referenceId))
			processes.add(link.getProviderId());

		ProductSystemNode productSystemNode = new ProductSystemNode(
				productSystem, this);
		for (Long id : processes)
			productSystemNode.add(createProcessNode(id));

		return productSystemNode;
	}

	private ProcessNode createProcessNode(long id) {
		ProcessDescriptor descriptor = Database.getCache()
				.getProcessDescriptor(id);
		ProcessNode processNode = new ProcessNode(descriptor);
		return processNode;
	}

	private GraphicalViewerConfigurator createGraphicalViewerConfigurator() {
		GraphicalViewerConfigurator configurator = new GraphicalViewerConfigurator(
				getGraphicalViewer());
		configurator.setActionRegistry(getActionRegistry());
		configurator.setCommandStack(getCommandStack());
		configurator.setModel(model);
		return configurator;
	}

	@Override
	protected void configureGraphicalViewer() {
		model = createModel();
		super.configureGraphicalViewer();
		configurator = createGraphicalViewerConfigurator();

		configurator.configureGraphicalViewer();
		configurator.configureActions();
		configurator.configureZoomManager();
		configurator.configureKeyHandler();
		configurator.configureContextMenu();
	}

	@Override
	protected void initializeGraphicalViewer() {
		configurator.initializeGraphicalViewer();
	}

	@Override
	public GraphicalViewer getGraphicalViewer() {
		return super.getGraphicalViewer();
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {

	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(final Class type) {
		if (type == ZoomManager.class) {
			return ((ScalableRootEditPart) getGraphicalViewer()
					.getRootEditPart()).getZoomManager();
		}
		if (type == IContentOutlinePage.class) {
			outline = new OutlinePage(model);
			outline.setEditDomain(getEditDomain());
			outline.setSelectionSynchronizer(getSelectionSynchronizer());
			return outline;
		}
		return super.getAdapter(type);
	}

	@Override
	public CommandStack getCommandStack() {
		CommandStack stack = super.getCommandStack();
		if (stack == null)
			stack = new CommandStack();
		return stack;
	}

	public GraphLayoutType getLayoutType() {
		return layoutType;
	}

	public ProductSystemNode getModel() {
		return model;
	}

	public OutlinePage getOutline() {
		return outline;
	}

	public ZoomManager getZoomManager() {
		return getRootEditPart().getZoomManager();
	}

	private ScalableRootEditPart getRootEditPart() {
		return (ScalableRootEditPart) getGraphicalViewer().getRootEditPart();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("firstTimeInitialized")) {
			initializeLinks();
		} else if (evt.getPropertyName().equals("RemovedConnections")) {
			// TODO adjust
			// if connections are removed from a process node it now can be
			// deleted
			// refreshDeleteAction((IStructuredSelection) evt.getNewValue());
		}
	}

	private void initializeLinks() {
		ProcessLink[] links = productSystem.getIncomingLinks(productSystem
				.getReferenceProcess().getId());
		for (ProcessLink processLink : links) {
			ProcessNode sourceNode = model.getProcessNode(processLink
					.getProviderId());
			ProcessNode targetNode = model.getProcessNode(processLink
					.getRecipientId());
			ConnectionLink link = new ConnectionLink();
			link.setSourceNode(sourceNode);
			link.setTargetNode(targetNode);
			link.setProcessLink(processLink);
			link.link();
		}
		getModel().getPart().refresh();
	}

	public void reset() {
		model = createModel();
		if (getGraphicalViewer() != null) {
			getGraphicalViewer().deselectAll();
			getGraphicalViewer().setContents(model);
		}
		// TODO adjust
		// if (showAction != null) {
		// showAction.setProductSystemNode(model);
		// }
		// if (hideAction != null) {
		// hideAction.setProductSystemNode(model);
		// }
		// if (bscAction != null) {
		// bscAction.setProductSystemNode(model);
		// }
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		super.selectionChanged(part, selection);
		List<ProcessNode> processNodes = new ArrayList<>();
		if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			for (Object o : structuredSelection.toArray())
				if (o instanceof ProcessPart)
					processNodes.add(((ProcessPart) o).getModel());
		}
		// ProcessNode[] nodes = processNodes.toArray(new
		// ProcessNode[processNodes
		// .size()]);
		// TODO adjust
		// // refresh the actions with the new selection
		// refreshOpenAction(nodes);
		// refreshMarkActions(nodes);
		// if (processNodes.size() == 1) {
		// refreshSupplyActions(processNodes.get(0));
		// } else {
		// refreshSupplyActions(null);
		// }
		// if (selection instanceof IStructuredSelection) {
		// refreshRemoveConnectionLinksAction(
		// (IStructuredSelection) selection,
		// processNodes.toArray(new ProcessNode[processNodes.size()]));
		// } else {
		// refreshRemoveConnectionLinksAction(new StructuredSelection(),
		// new ProcessNode[0]);
		// }
		// if (selection instanceof IStructuredSelection) {
		// refreshDeleteAction((IStructuredSelection) selection);
		// refreshGetLinksAction((IStructuredSelection) selection);
		// }
	}

	@Override
	public void setFocus() {
		if (getGraphicalViewer() == null)
			return;
		if (getGraphicalViewer().getControl() == null)
			return;
		super.setFocus();
	}

	public boolean isRouted() {
		return routed;
	}

	public void setRouted(boolean routed) {
		this.routed = routed;
		ConnectionRouter router = ConnectionRouter.NULL;
		if (routed)
			router = TreeConnectionRouter.get();
		for (ProcessNode node : model.getChildren())
			for (ConnectionLink link : node.getLinks())
				link.getFigure().setConnectionRouter(router);
	}

}