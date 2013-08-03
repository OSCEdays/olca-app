package org.openlca.app.viewers;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractTableViewer.IModelChangedListener.Type;
import org.openlca.app.viewers.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.modify.TextCellModifier;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;

import com.google.common.base.Objects;

public class UnitViewer extends AbstractTableViewer<Unit> {

	private static final String UNIT_CONVERSION_FACTOR = Messages.Units_ConversionFactor;
	private static final String UNIT_DESCRIPTION = Messages.Common_Description;
	private static final String UNIT_FORMULA = Messages.Units_Formula;
	private static final String UNIT_IS_REFERENCE = Messages.Units_IsReference;
	private static final String UNIT_NAME = Messages.Common_Name;
	private static final String UNIT_SYNONYMS = Messages.Units_Synonyms;
	private static final String[] COLUMN_HEADERS = { UNIT_NAME,
			UNIT_DESCRIPTION, UNIT_SYNONYMS, UNIT_CONVERSION_FACTOR,
			UNIT_FORMULA, UNIT_IS_REFERENCE };

	private UnitGroup unitGroup;

	public UnitViewer(Composite parent) {
		super(parent);
		getCellModifySupport().support(UNIT_NAME, new NameModifier());
		getCellModifySupport().support(UNIT_DESCRIPTION,
				new DescriptionModifier());
		getCellModifySupport().support(UNIT_SYNONYMS, new SynonymsModifier());
		getCellModifySupport().support(UNIT_CONVERSION_FACTOR,
				new ConversionFactorModifier());
		getCellModifySupport().support(UNIT_IS_REFERENCE,
				new ReferenceModifier());
		getViewer().refresh(true);
	}

	public void setInput(UnitGroup unitGroup) {
		super.setInput(unitGroup.getUnits().toArray(
				new Unit[unitGroup.getUnits().size()]));
		this.unitGroup = unitGroup;
		getViewer().refresh(true);
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new UnitLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	@OnCreate
	protected void onCreate() {
		Unit unit = new Unit();
		unit.setName("newUnit");
		unit.setConversionFactor(1d);
		fireModelChanged(Type.CREATE, unit);
		setInput(unitGroup);
	}

	@OnRemove
	protected void onRemove() {
		for (Unit unit : getAllSelected())
			if (!Objects.equal(unitGroup.getReferenceUnit(), unit))
				fireModelChanged(Type.REMOVE, unit);
		setInput(unitGroup);
	}

	private class UnitLabelProvider implements ITableLabelProvider,
			ITableFontProvider {

		private Font boldFont;

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
			if (boldFont != null && !boldFont.isDisposed())
				boldFont.dispose();
		}

		@Override
		public Image getColumnImage(Object element, int column) {
			if (column == 0)
				return ImageType.UNIT_GROUP_ICON.get();
			if (column != 5)
				return null;
			Unit refUnit = unitGroup != null ? unitGroup.getReferenceUnit()
					: null;
			if (refUnit != null && refUnit.equals(element))
				return ImageType.CHECK_TRUE.get();
			return ImageType.CHECK_FALSE.get();
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Unit))
				return null;
			Unit unit = (Unit) element;
			switch (columnIndex) {
			case 0:
				return unit.getName();
			case 1:
				return unit.getDescription();
			case 2:
				return unit.getSynonyms();
			case 3:
				return Numbers.format(unit.getConversionFactor());
			case 4:
				return getFormulaText(unit);
			default:
				return null;
			}
		}

		private String getFormulaText(Unit unit) {
			Unit refUnit = unitGroup != null ? unitGroup.getReferenceUnit()
					: null;
			if (refUnit == null)
				return null;
			String amount = "[" + unit.getName() + "]";
			String refAmount = "[" + refUnit.getName() + "]";
			String factor = Numbers.format(unit.getConversionFactor());
			return amount + " = " + factor + " * " + refAmount;
		}

		@Override
		public Font getFont(Object element, int columnIndex) {
			Unit refUnit = unitGroup != null ? unitGroup.getReferenceUnit()
					: null;
			if (refUnit != null && refUnit.equals(element)) {
				if (boldFont == null)
					boldFont = UI.boldFont(getViewer().getTable());
				return boldFont;
			}
			return null;
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}
	}

	private class NameModifier extends TextCellModifier<Unit> {

		@Override
		protected String getText(Unit element) {
			return element.getName();
		}

		@Override
		protected void setText(Unit element, String text) {
			element.setName(text);
			fireModelChanged(Type.CHANGE, element);
		}

	}

	private class DescriptionModifier extends TextCellModifier<Unit> {

		@Override
		protected String getText(Unit element) {
			return element.getDescription();
		}

		@Override
		protected void setText(Unit element, String text) {
			element.setDescription(text);
			fireModelChanged(Type.CHANGE, element);
		}
	}

	private class SynonymsModifier extends TextCellModifier<Unit> {

		@Override
		protected String getText(Unit element) {
			return element.getSynonyms();
		}

		@Override
		protected void setText(Unit element, String text) {
			element.setSynonyms(text);
			fireModelChanged(Type.CHANGE, element);
		}

	}

	private class ConversionFactorModifier extends TextCellModifier<Unit> {

		@Override
		protected String getText(Unit element) {
			return Double.toString(element.getConversionFactor());
		}

		@Override
		protected void setText(Unit element, String text) {
			try {
				element.setConversionFactor(Double.parseDouble(text));
				fireModelChanged(Type.CHANGE, element);
			} catch (NumberFormatException e) {

			}
		}
	}

	private class ReferenceModifier extends CheckBoxCellModifier<Unit> {

		@Override
		protected boolean isChecked(Unit element) {
			return unitGroup != null
					&& Objects.equal(unitGroup.getReferenceUnit(), element);
		}

		@Override
		protected void setChecked(Unit element, boolean value) {
			if (value) {
				unitGroup.setReferenceUnit(element);
				fireModelChanged(Type.CHANGE, element);
			}
		}

	}

}