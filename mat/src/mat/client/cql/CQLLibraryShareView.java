package mat.client.cql;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableCaptionElement;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

import mat.client.CqlLibraryPresenter;
import mat.client.CustomPager;
import mat.client.ImageResources;
import mat.client.shared.CQLLibraryNameLabel;
import mat.client.shared.CustomButton;
import mat.client.shared.ErrorMessageAlert;
import mat.client.shared.LabelBuilder;
import mat.client.shared.MatCheckBoxCell;
import mat.client.shared.MatSimplePager;
import mat.client.shared.SaveCancelButtonBar;
import mat.client.shared.SearchWidget;
import mat.client.shared.SpacerWidget;
import mat.client.util.CellTableUtility;
import mat.model.clause.ShareLevel;
import mat.model.cql.CQLLibraryShareDTO;

public class CQLLibraryShareView implements CqlLibraryPresenter.ShareDisplay{

	private static final int PAGE_SIZE = 25;
	/** The button bar. */
	private SaveCancelButtonBar buttonBar = new SaveCancelButtonBar("cqlShare");
	/** The cell table panel. */
	private VerticalPanel cellTablePanel = new VerticalPanel();
	/** The content. */
	private FlowPanel mainPanel = new FlowPanel();
	
	private ErrorMessageAlert errorMessages = new ErrorMessageAlert();
	
	/** The measure name label. */
	private CQLLibraryNameLabel cqLLibnNameLabel = new CQLLibraryNameLabel();
	
	/** The private check. *//*
	private CheckBox privateCheck = new CheckBox();
	*/
	
	/** The measure search filter widget. */
	private SearchWidget searchWidget = new SearchWidget("Search", 
            "Search", "searchWidget");
	
	/** Zoom Button for Showing Search Widget. */
	private CustomButton zoomButton = (CustomButton) getImage("Search",
			ImageResources.INSTANCE.search_zoom(), "Search");

	/**
	 * Instantiates a new manage measure share view.
	 */
	public CQLLibraryShareView() {
		zoomButton.getElement().getStyle().setMarginLeft(30, Unit.PX);
		zoomButton.getElement().setId("CqlzoomButton_CustomButton");
		mainPanel.setStylePrimaryName("contentPanel");
		mainPanel.addStyleName("leftAligned");
		mainPanel.add(new SpacerWidget());
		buttonBar.getSaveButton().setText("Save and Continue");
		HorizontalPanel horizontalPanel = new HorizontalPanel();
		horizontalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		horizontalPanel.getElement().setId("horizontalPanel_HorizontalPanel");
		horizontalPanel.add(cqLLibnNameLabel);
		/* Private CQL Library Functionality is not implemented as a part of MAT-8475*/
		/*InlineLabel privateCheckLabel = new InlineLabel("Private CQL Library");
		privateCheck.setStyleName("gwt-CheckBox");
		privateCheckLabel.setStyleName("privateCheckLabel");
		horizontalPanel.add(privateCheck);
		horizontalPanel.add(privateCheckLabel);*/
		//horizontalPanel.add(searchWidget);
		horizontalPanel.setStyleName("horizontalPanel");
		mainPanel.add(searchWidget);
		mainPanel.add(new SpacerWidget());
		mainPanel.add(horizontalPanel);
		
		cellTablePanel.getElement().setId("cellTablePanel_VerticalPanel");
		cellTablePanel.setWidth("77%");
		mainPanel.add(cellTablePanel);
		mainPanel.add(new SpacerWidget());
		mainPanel.add(buttonBar);
		
	}
	/** Adds the column to table.
	 * @param cellTable the cell table
	 * @return the cell table */
	private CellTable<CQLLibraryShareDTO> addColumnToTable(final CellTable<CQLLibraryShareDTO> cellTable) {
		
		Column<CQLLibraryShareDTO, SafeHtml> userNameColumn = new Column<CQLLibraryShareDTO, SafeHtml>(new SafeHtmlCell()) {
			@Override
			public SafeHtml getValue(CQLLibraryShareDTO object) {
				String title = "User Name :" + object.getFirstName() + " " + object.getLastName();
				String name = object.getFirstName() + " " + object.getLastName();
				return CellTableUtility.getColumnToolTip(name, title);
			}
		};
		cellTable.addColumn(userNameColumn, SafeHtmlUtils.fromSafeConstant("<span title='User Name'>"
				+ "User Name" + "</span>"));
		Column<CQLLibraryShareDTO, SafeHtml> organizationColumn = new Column<CQLLibraryShareDTO, SafeHtml>(new SafeHtmlCell()) {
			@Override
			public SafeHtml getValue(CQLLibraryShareDTO object) {
				String title = "Organization :" + object.getOrganizationName();
				String name = object.getOrganizationName();
				return CellTableUtility.getColumnToolTip(name, title);
			}
		};
		cellTable.addColumn(organizationColumn, SafeHtmlUtils.fromSafeConstant("<span title='Organization'>"
				+ "Organization" + "</span>"));
		Cell<Boolean> shareTransferCB = new MatCheckBoxCell();
		Column<CQLLibraryShareDTO, Boolean> shareColumn = new Column<CQLLibraryShareDTO, Boolean>(shareTransferCB) {
			@Override
			public Boolean getValue(CQLLibraryShareDTO object) {
				String currentShare = object.getShareLevel();
				Boolean shareValue = false;
				if (ShareLevel.VIEW_ONLY_ID.equals(currentShare)) {
					shareValue = false;
				} else if (ShareLevel.MODIFY_ID.equals(currentShare)) {
					shareValue = true;
				}
				return shareValue;
			}
			/*
			 * @Override public void render(com.google.gwt.cell.client.Cell.Context context, CQLLibraryShareDTO data, SafeHtmlBuilder sb) {
			 * String title = "<div title=\"Select User " + data.getFirstName() + " " + data.getLastName() + " to Share Measure." +
			 * "\"></div>"; sb.appendHtmlConstant(title).toSafeHtml(); super.render(context, data, sb); }
			 */
		};
		shareColumn.setFieldUpdater(new FieldUpdater<CQLLibraryShareDTO, Boolean>() {
			@Override
			public void update(int index, CQLLibraryShareDTO object, Boolean value) {
				if (value) {
					object.setShareLevel(ShareLevel.MODIFY_ID);
				} else {
					object.setShareLevel(ShareLevel.VIEW_ONLY_ID);
				}
			}
		});
		cellTable.addColumn(shareColumn, SafeHtmlUtils.fromSafeConstant("<span title='Check for Share'>" + "Share"
				+ "</span>"));
		return cellTable;
	}
	
	/* (non-Javadoc)
	 * @see mat.client.measure.ManageMeasurePresenter.BaseDisplay#asWidget()
	 */
	@Override
	public Widget asWidget() {
		return mainPanel;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see mat.client.measure.ManageMeasurePresenter.ShareDisplay#buildDataTable(mat.client.shared.search.SearchResults)
	 */
	@Override
	public void buildCQLLibraryShareTable(List<CQLLibraryShareDTO> data) {

		cellTablePanel.clear();
		cellTablePanel.setStyleName("cellTablePanel");
		Label cellTableHeader = new Label("Select users with whom you wish to share modify access.");
		cellTableHeader.getElement().setId("CQLLibraryShareTableHeader_Label");
		cellTableHeader.setStyleName("recentSearchHeader");
		cellTableHeader.getElement().setAttribute("tabIndex", "0");
		
		if ((data != null)
				&& (data.size() > 0)) {
			CellTable<CQLLibraryShareDTO> cellTable = new CellTable<CQLLibraryShareDTO>();
			ListDataProvider<CQLLibraryShareDTO> sortProvider = new ListDataProvider<CQLLibraryShareDTO>();
			List<CQLLibraryShareDTO> measureShareList = new ArrayList<CQLLibraryShareDTO>();
			measureShareList.addAll(data);
			cellTable.setPageSize(PAGE_SIZE);
			cellTable.redraw();
			cellTable.setRowCount(measureShareList.size(), true);
			// cellTable.setSelectionModel(getSelectionModelWithHandler());
			sortProvider.refresh();
			sortProvider.getList().addAll(measureShareList);
			cellTable = addColumnToTable(cellTable);
			sortProvider.addDataDisplay(cellTable);
			com.google.gwt.dom.client.TableElement elem = cellTable.getElement().cast();
			TableCaptionElement caption = elem.createCaption();
			caption.appendChild(cellTableHeader.getElement());
			CustomPager.Resources pagerResources = GWT.create(CustomPager.Resources.class);
			MatSimplePager spager = new MatSimplePager(CustomPager.TextLocation.CENTER, pagerResources, false, 0, true,"cqlShare");
			spager.setPageStart(0);
			spager.setDisplay(cellTable);
			spager.setPageSize(PAGE_SIZE);
			cellTable.setWidth("100%");
			cellTable.setColumnWidth(0, 40.0, Unit.PCT);
			cellTable.setColumnWidth(1, 40.0, Unit.PCT);
			cellTable.setColumnWidth(2, 20.0, Unit.PCT);
			Label invisibleLabel = (Label) LabelBuilder.buildInvisibleLabel(
					"measureShareSummary",
					"In the Following Measure sharing table, User name is given in first column, Organization "
							+ "in second column and Share in third column with Check boxes positioned to the "
							+ "right of the table.");
			cellTable.getElement().setAttribute("id", "measureShareCellTable");
			cellTable.getElement().setAttribute("aria-describedby", "measureShareSummary");
			cellTablePanel.add(invisibleLabel);
			cellTablePanel.add(cellTable);
			cellTablePanel.add(new SpacerWidget());
			cellTablePanel.add(spager);
		} else {
			HTML desc = new HTML("<p> No Users available for sharing.</p>");
			cellTablePanel.add(cellTableHeader);
			cellTablePanel.add(new SpacerWidget());
			cellTablePanel.add(desc);
		}
		
		
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see mat.client.measure.ManageMeasurePresenter.ShareDisplay#getCancelButton()
	 */
	@Override
	public HasClickHandlers getCancelButton() {
		return buttonBar.getCancelButton();
	}
	
	/* (non-Javadoc)
	 * @see mat.client.measure.ManageMeasurePresenter.BaseDisplay#getErrorMessageDisplay()
	 */
	@Override
	public ErrorMessageAlert getErrorMessageDisplay() {
		return errorMessages;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see mat.client.measure.ManageMeasurePresenter.ShareDisplay#getShareButton()
	 */
	@Override
	public HasClickHandlers getSaveButton() {
		return buttonBar.getSaveButton();
	}
	
	/* (non-Javadoc)
	 * @see mat.client.measure.ManageMeasurePresenter.ShareDisplay#privateCheckbox()
	 */
	/*@Override
	public HasValueChangeHandlers<Boolean> privateCheckbox() {
		return privateCheck;
	}*/
	/*
	 * (non-Javadoc)
	 * 
	 * @see mat.client.measure.ManageMeasurePresenter.ShareDisplay#setMeasureName(java.lang.String)
	 */
	@Override
	public void setCQLibraryName(String name) {
		cqLLibnNameLabel.setCQLLibraryName(name);
	}
	/* (non-Javadoc)
	 * @see mat.client.measure.ManageMeasurePresenter.ShareDisplay#setPrivate(boolean)
	 */
	/*@Override
	public void setPrivate(boolean isPrivate) {
		privateCheck.setValue(isPrivate);
	}*/
	
	private Widget getImage(String action, ImageResource url, String key) {
		CustomButton image = new CustomButton();
		image.removeStyleName("gwt-button");
		image.setStylePrimaryName("invisibleButtonTextMeasureLibrary");
		image.setTitle(action);
		image.setResource(url, action);
		image.getElement().setAttribute("id", "CQLLibVersionViewSearchButton");
		return image;
	}
	
	@Override
	public CustomButton getZoomButton() {
		return zoomButton;
	}
	
	@Override
	public SearchWidget getSearchWidget() {
		return searchWidget;
	}
}