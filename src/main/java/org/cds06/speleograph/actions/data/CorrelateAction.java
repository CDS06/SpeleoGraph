package org.cds06.speleograph.actions.data;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.cds06.speleograph.I18nSupport;
import org.cds06.speleograph.SpeleoGraphApp;
import org.cds06.speleograph.data.Item;
import org.cds06.speleograph.data.Series;
import org.cds06.speleograph.utils.DateSelector;
import org.cds06.speleograph.utils.FormDialog;
import org.jetbrains.annotations.NonNls;
import org.jfree.data.time.DateRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Created by Gabriel Augendre.
 * Allow the user to recalibrate a Pressure Serie.
 * Recalibration will align a serie of data on another.
 */
public class CorrelateAction extends AbstractAction {

    private final Series series;

    /**
     * A value in minute.
     * We consider two points of data taken at the same moment if they are spaced-out of at maximum this value.
     */
    private static final int TEMPORAL_RANGE_ACCEPTED = 3;

    /**
     * Logger for errors and info.
     */
    @SuppressWarnings("UnusedDeclaration")
    @NonNls
    private static Logger log = LoggerFactory.getLogger(SpeleoGraphApp.class);

    public CorrelateAction(Series series) {
        super();
        putValue(NAME, I18nSupport.translate("actions.correlate"));
        this.series = series;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PromptDialog dialog = new PromptDialog();
        DateRange range = series.getRange();
        dialog.startDateSelector.setDate(range.getLowerDate());
        dialog.endDateSelector.setDate(range.getUpperDate());
        dialog.setVisible(true);
    }



    private class PromptDialog extends FormDialog {

        private DateSelector startDateSelector = new DateSelector();
        private DateSelector endDateSelector = new DateSelector();
        private JComboBox<Series> seriesList = new JComboBox<>();
        {
            ListModel<Series> listModel = SpeleoGraphApp.getInstance().getSeriesList().getModel();
            for (int i = 0; i < listModel.getSize(); i++) {
                Series item = listModel.getElementAt(i);
                if (!item.equals(series) && item.isPressure())
                    seriesList.addItem(listModel.getElementAt(i));
            }
        }
        private FormLayout layout = new FormLayout("p:grow", "p,4dlu,p,4dlu,p,4dlu,p,4dlu,p,6dlu,p"); //NON-NLS

        private PromptDialog() {
            super();
            construct();
            this.setTitle(I18nSupport.translate("actions.correlate.title"));
        }

        @Override
        protected void setup() {
            PanelBuilder builder = new PanelBuilder(layout, getPanel());
            builder.addLabel(I18nSupport.translate("actions.correlate.selectRange"));
            builder.nextLine(2);
            builder.add(startDateSelector);
            builder.nextLine(2);
            builder.addLabel(I18nSupport.translate("actions.limit.label2"));
            builder.nextLine(2);
            builder.add(endDateSelector);
            builder.nextLine(2);
            builder.add(seriesList);
            builder.nextLine(2);
            builder.add(new JButton(new AbstractAction() {

                {
                    putValue(NAME, I18nSupport.translate("ok"));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    validateForm();
                }
            }));

            Dimension dim = getPanel().getPreferredSize();
            getPanel().setPreferredSize(new Dimension(dim.width + 50, dim.height));
        }

        @Override
        protected void validateForm() {
            ArrayList<Item> itemsStandards = ((Series) seriesList.getSelectedItem()).extractSubSerie(startDateSelector.getDate(), endDateSelector.getDate());
            ArrayList<Item> itemsToCorrelate = series.extractSubSerie(startDateSelector.getDate(), endDateSelector.getDate());

            double difference = 0;
            int itemCount = 0;

            for (Item item : itemsStandards) {
                ListIterator<Item> iter = itemsToCorrelate.listIterator();
                while (iter.hasNext()) {
                    Item itemCor = iter.next();
                    // Divided by 60000 to transform milliseconds into minutes.
                    if ((item.getDate().getTime() - itemCor.getDate().getTime())/60000 > TEMPORAL_RANGE_ACCEPTED) {
                        iter.remove();
                    } else if ((itemCor.getDate().getTime() - item.getDate().getTime())/60000 > TEMPORAL_RANGE_ACCEPTED) {
                        break;
                    } else {
                        difference += (itemCor.getValue() - item.getValue());
                        itemCount++;
                        iter.remove();
                    }
                }
            }
            final double differenceMoyenne = difference/itemCount;
            ArrayList<Item> newItems = new ArrayList<>(series.getItems().size());
            for (Item item : series.getItems()) {
                newItems.add(new Item (item.getSeries(), item.getDate(), item.getValue() - differenceMoyenne));
            }
            series.setItems(newItems, (String) getValue(NAME));
            InfoDialog iD = new InfoDialog(differenceMoyenne);
            iD.setVisible(true);
            setVisible(false);
        }

        @Override
        protected FormLayout getFormLayout() {
            return null;
        }

        private class InfoDialog extends FormDialog {

            private FormLayout layout = getFormLayout();
            double differenceMoyenne;

            public InfoDialog(double differenceMoyenne) {
                super();
                this.differenceMoyenne = differenceMoyenne;
                construct();
            }
            @Override
            protected void setup() {
                PanelBuilder builder = new PanelBuilder(layout, getPanel());
                setTitle(I18nSupport.translate("confirm"));
                final String[] diff = ((Double) Math.abs(differenceMoyenne)).toString().split("\\.");
                final String diff2 = diff[0] + "," + diff[1].substring(0,4);
                builder.addLabel("<HTML>"+"<center>" + I18nSupport.translate("actions.correlate.space") +" :<br>"+diff2+"</center><HTML>");
                builder.nextLine(2);
                builder.add(new JButton(new AbstractAction() {
                    {
                        putValue(NAME, I18nSupport.translate("ok"));
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        validateForm();
                    }
                }));
            }

            @Override
            protected void validateForm() {
                setVisible(false);
            }

            @Override
            protected FormLayout getFormLayout() {
                return new FormLayout("p:grow", "p:grow,6dlu,p");
            }
        }
    }
}
