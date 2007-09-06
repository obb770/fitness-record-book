/*
Copyright 2007 Ofer Barkai

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package fitness.client;

import java.util.Date;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Comparator;
import java.util.Collections;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.RootPanel;

public class Fitness implements EntryPoint {

    static void alert(String message) {
        System.err.println(message);
    }

    static Constants c = (Constants)GWT.create(Constants.class);
    static TabPanel tp;
    static Totals totals;
    static DB food;
    static DB pA;
    static DB weight;
    static Options options;
    
    public void onModuleLoad() {
        totals = new Totals();
        food = new DB(Model.food, 3, new CalRec(c.editFood(), c.foodUnits())); 
        pA = new DB(Model.pA, 3, new CalRec(c.editPA(), c.pAUnits())); 
        weight = new DB(Model.weight, 2, new WeightRec(c.editWeight()));
        options = new Options();

        tp = new TabPanel() {;
            {
                add(Fitness.totals, c.totals());
                add(Fitness.food, c.food());
                add(Fitness.pA, c.pA());
                add(Fitness.weight, c.weight());
                selectTab(0);
            }

            public boolean onBeforeTabSelected(SourcesTabEvents sender,
                                               int tabIndex) {
                Widget tab = getWidget(tabIndex);
                ((Page)tab).update();
                return super.onBeforeTabSelected(sender, tabIndex);
            }
        };
        RootPanel.get().add(tp);
    }

    static class Page extends DockPanel {
        FlowPanel buttons = new FlowPanel();

        Page() {
            setSpacing(4);
            add(buttons, SOUTH);
            setCellHorizontalAlignment(buttons, ALIGN_CENTER);
        }

        void setContent(Widget w, boolean fixed) {
            if (fixed) {
                w = new ScrollPanel(w);
                w.setHeight("15em"); //FIXME: should this be in the style sheet?
            }
            add(w, CENTER);
        }

        void setContent(Widget w) {setContent(w, true);}

        void addButton(String name, ClickListener cl) {
            Button b = new Button(name);
            b.addClickListener(cl);
            buttons.add(b);
        }

        void update() {}
    }

    static class DateView extends Composite implements ChangeListener {
        final TextBox tb = new TextBox();
        Date date;

        DateView() {
            tb.setVisibleLength(8);
            tb.addChangeListener(this);
            setDate(today());
            initWidget(tb);
        }

        String getText() {return tb.getText();}

        void setDate(Date date) { // FIXME: DateTimeFormat? locale?
            this.date = date;
            int year = date.getYear();
            if (year >= 100) {
                year -= 100;
            }
            if (date.getTime() > 0) {
                tb.setText((date.getMonth() + 1) + "/" +
                            date.getDate() + "/" + 
                            (year <= 9 ? "0" : "") + year);
            }
            else {
                tb.setText("");
            }
        }

        void setReadOnly(boolean readonly) {tb.setReadOnly(readonly);}


        public void onChange(Widget sender) {// FIXME: DateTimeFormat? locale?
            date.setTime(Date.parse(((DateView)sender).getText()));
        }

        static Date today(long time) { // FIXME: too many Date instances?
            Date today = new Date(time < 0 ? System.currentTimeMillis() : time);
            return new Date(today.getYear(), today.getMonth(), today.getDate());
        }

        static Date today() {
            return today(-1);
        }

        // The date n days ago
        static Date dayBefore(int n) {
            return today(today().getTime() - n * 24 * 60 * 60 * 1000);
        }

        static Date yesterday() {
            return dayBefore(1);
        }

        static Date thisWeek() {
            return dayBefore(today().getDay());
        }

        static Date thisMonth() {
            return dayBefore(today().getDate() - 1);
        }
    }

    static class Totals extends Page implements ChangeListener {
        final Grid tg;
        final DateView fromDate = new DateView();
        final DateView thruDate = new DateView();
        final ListBox showFor = new ListBox();

        Totals() {
            VerticalPanel vp = new VerticalPanel();
            Grid g = new Grid(1, 2);
            g.setText(0, 0, c.showTotalsFor());
            showFor.addItem(c.today());
            showFor.addItem(c.yesterday());
            showFor.addItem(c.thisWeek());
            showFor.addItem(c.thisMonth());
            showFor.addItem(c.allData());
            showFor.addItem(c.range());
            showFor.addChangeListener(this);
            g.setWidget(0, 1, showFor);
            g.setWidth("100%");
            vp.add(g);

            g = new Grid(1, 3);
            g.setWidget(0, 0, fromDate);
            g.setText(0, 1, c.thru());
            g.setWidget(0, 2, thruDate);
            g.setWidth("100%");
            vp.add(g);

            tg = new Grid(7, 2);
            tg.setText(0, 0, c.caloriesIn());
            tg.setText(1, 0, c.pACalories());
            tg.setText(2, 0, c.metabolism());
            tg.setText(3, 0, c.netCalories());
            tg.setText(4, 0, c.behavioralWeight());
            tg.setText(5, 0, c.daysInRange());
            tg.setText(6, 0, c.calsLeftToEat());
            tg.setWidth("100%");
            update();
            vp.add(tg);

            setContent(vp);
            
            addButton(c.options(), new ClickListener() {
                public void onClick(Widget sender) {
                    Fitness.options.show();
                }
            });
        }

        public void onChange(Widget sender) {
            ListBox lb = (ListBox)sender;
            String showFor = lb.getItemText(lb.getSelectedIndex());
            fromDate.setReadOnly(true);
            thruDate.setReadOnly(true);
            thruDate.setDate(DateView.today());
            if      (showFor.equals(c.today())) {
                fromDate.setDate(DateView.today());
            }
            else if (showFor.equals(c.yesterday())) {
                fromDate.setDate(DateView.yesterday());
                thruDate.setDate(DateView.yesterday());
            }
            else if (showFor.equals(c.thisWeek())) {
                fromDate.setDate(DateView.thisWeek());
            }
            else if (showFor.equals(c.thisMonth())) {
                fromDate.setDate(DateView.thisMonth());
            }
            else if (showFor.equals(c.allData())) {
                fromDate.setDate(DateView.today(0));
            }
            else if (showFor.equals(c.range())) {
                fromDate.setDate(DateView.today(0));
                fromDate.setReadOnly(false);
                thruDate.setReadOnly(false);
            }
        }

        void update() {
            Model.Totals.update(fromDate.getText(), thruDate.getText());
            tg.setText(0, 1, Model.Totals.caloriesIn);
            tg.setText(1, 1, Model.Totals.pACalories);
            tg.setText(2, 1, Model.Totals.metabolism);
            tg.setText(3, 1, Model.Totals.netCalories);
            tg.setText(4, 1, Model.Totals.behavioralWeight);
            tg.setText(5, 1, Model.Totals.daysInRange);
            tg.setText(6, 1, Model.Totals.calsLeftToEat);
        }
    }

    static class DB extends Page implements TableListener {
        final Model.DB mdb;
        final int nCol;
        final Record record;
        final Grid g;

        DB(Model.DB mdb, int nCol, Record record) {
            this.mdb = mdb;
            this.nCol = nCol;
            this.record = record;
            record.setDB(this);
            record.setModelDB(mdb);
            g = new Grid(0, nCol);
            g.setWidth("100%");
            g.addTableListener(this);
            setContent(g);

            addButton(c.newButton(), new ClickListener() {
                public void onClick(Widget sender) {
                    DB.this.record.show();
                }
            });
        }

        void update() {
            g.resizeRows(mdb.size());
            int i = 0;
            for (ListIterator it = mdb.listIterator(); it.hasNext(); i++) {
                Model.Record r = (Model.Record)it.next();
                for (int j = 0; j < nCol; j++) {
                    g.setText(i, j, r.getField(j));
                }
            }
        }

        public void onCellClicked(SourcesTableEvents sender, 
                                  int row, int cell) {
            record.init(row);
            record.show();
        }
    }

    static abstract class Dialog extends DialogBox {
        final private Page page = new Page();

        Dialog(String title) {
            setText(title);
            setWidget(page);
            
            page.addButton(c.oK(), new ClickListener() {
                public void onClick(Widget sender) {
                    accept();
                }
            });
            page.addButton(c.cancel(), new ClickListener() {
                public void onClick(Widget sender) {
                    dismiss();
                }
            });

            int left = RootPanel.get().getAbsoluteLeft() + 30;
            int top = RootPanel.get().getAbsoluteTop() + 30;
            setPopupPosition(left, top);
        }

        protected void setContent(Widget center) {
            page.setContent(center, false);
        }

        abstract void accept();

        void dismiss() {hide();}
    }

    static abstract class Record extends Dialog {
        protected final Grid g = new Grid(2, 2);
        protected final TextBox date = new TextBox();
        protected DB db;
        protected Model.DB mdb;
        protected int row = -1;

        Record(String title) {
            super(title);
            g.setText(0, 0, c.date());
            g.setWidget(0, 1, date);
            setContent(g);
        }

        void setDB(DB db) {this.db = db;}
        void setModelDB(Model.DB mdb) {this.mdb = mdb;}

        void apply(Model.Record mr) {
            if (row >= 0 && mdb.compare(mr, mdb.get(row)) == 0) {
                mdb.set(row, mr);
            }
            else {
                mdb.add(mr);
            }
            db.update();
            dismiss();
        }

        void dismiss() {
            row = -1;
            date.setText("");
            super.dismiss();
        }

        void init(int row) {
            this.row = row;
            date.setText(((Model.Record)mdb.get(row)).dateStr);
        }
    }

    static class CalRec extends Record {
        private final TextBox desc = new TextBox();
        private final TextBox quantity = new TextBox();
        private final ListBox unit = new ListBox();
        private final TextBox calPerUnit = new TextBox();

        CalRec(String title, String[] units) {
            super(title);
            g.resizeRows(5);

            g.setText(1, 0, c.desc());
            g.setWidget(1, 1, desc);

            g.setText(2, 0, c.quantity());
            g.setWidget(2, 1, quantity);

            g.setText(3, 0, c.unit());
            for (int i = 0; i < units.length; i++) {
                unit.addItem(units[i]);
            }
            g.setWidget(3, 1, unit);

            g.setText(4, 0, c.calPerUnit());
            g.setWidget(4, 1, calPerUnit);
        }

        void accept() {
            try {
                Model.CalRec mcr = 
                    new Model.CalRec(date.getText(), desc.getText(), 
                                     Double.parseDouble(quantity.getText()),
                                     Double.parseDouble(calPerUnit.getText()));
                apply(mcr);
            }
            catch (NumberFormatException nfe) {
                alert(c.badQuantityOrCalPerUnit());
            }
            catch (IllegalArgumentException iae) {
                alert(c.badDate());
            }
        }

        void dismiss() {
            desc.setText("");
            quantity.setText("");
            calPerUnit.setText("");
            super.dismiss();
        }

        void init(int row) {
            super.init(row);
            Model.CalRec mcr = (Model.CalRec)mdb.get(row);
            desc.setText(mcr.desc);
            quantity.setText("" + mcr.quantity);
            //FIXME: set unit (need to save it first in the model)
            calPerUnit.setText("" + mcr.calPerUnit);
        }
    }

    static class WeightRec extends Record {
        private final TextBox weight = new TextBox();

        WeightRec(String title) {
            super(title);
            g.setText(1, 0, c.weightRec());
            g.setWidget(1, 1, weight);
        }

        void accept() {
            try {
                Model.WeightRec mwr = 
                    new Model.WeightRec(date.getText(), 
                                        Double.parseDouble(weight.getText()));
                apply(mwr);
            }
            catch (NumberFormatException nfe) {
                alert(c.badWeight());
            }
            catch (IllegalArgumentException iae) {
                alert(c.badDate());
            }
        }

        void dismiss() {
            weight.setText("");
            super.dismiss();
        }

        void init(int row) {
            super.init(row);
            weight.setText(((Model.WeightRec)mdb.get(row)).weight);
        }
    }

    static class Options extends Dialog {
        Options() {
            super(c.options());

            VerticalPanel vp = new VerticalPanel();
            Grid g = new Grid(6, 2);

            g.setText(0, 0, c.metabolismOpt());
            TextBox tb = new TextBox();
            tb.setText(Model.metabolismOpt());
            g.setWidget(0, 1, tb);

            g.setText(1, 0, c.goal());
            FlowPanel p = new FlowPanel();
            RadioButton rb = new RadioButton(c.goal(), c.weightOptVal());
            p.add(rb);
            rb.setChecked(true); // FIXME: should be in the model
            rb = new RadioButton(c.goal(), c.deficit());
            p.add(rb);
            g.setWidget(1, 1, p);

            g.setText(2, 0, c.goalWeight());
            tb = new TextBox();
            tb.setText(Model.goalWeight());
            g.setWidget(2, 1, tb);

            g.setText(3, 0, c.goalDeficit());
            tb = new TextBox();
            tb.setText(Model.goalDeficit());
            g.setWidget(3, 1, tb);

            g.setText(4, 0, c.weightOpt());
            p = new FlowPanel();
            rb = new RadioButton(c.weightOpt(), c.pounds());
            p.add(rb);
            rb = new RadioButton(c.weightOpt(), c.kilograms());
            p.add(rb);
            rb.setChecked(true); // FIXME: should be in the model
            g.setWidget(4, 1, p);

            g.setText(5, 0, c.historyDays());
            tb = new TextBox();
            tb.setText(Model.historyDays());
            g.setWidget(5, 1, tb);

            g.setWidth("100%");
            vp.add(g);

            setContent(vp);

        }

        void accept() {}
    }

    static class Model {
        static class Totals {
            static String caloriesIn;
            static String pACalories;
            static String metabolism;
            static String netCalories;
            static String behavioralWeight;
            static String daysInRange;
            static String calsLeftToEat;

            static void update(String fromDate, String thruDate) {
                caloriesIn = "1604.0";
                pACalories = "150.0";
                metabolism = "2032.8";
                netCalories = "-578.8";
                behavioralWeight = "60.0";
                daysInRange = "1.0";
                calsLeftToEat = "416.0";
            }
        }


        // Options
        static String metabolismOpt() {
            return "11.0";
        }

        static String goalWeight() {
            return "170.0";
        }

        static String goalDeficit() {
            return "0.0";
        }

        static String historyDays() {
            return "90";
        }

        // Records
        static abstract class Record {
            final Date date;
            final String dateStr;
            final String dateViewStr;

            Record(String dateStr) {
                this.dateStr = dateStr;
                this.date = new Date(dateStr); // FIXME: use DateTimeFormat
                                               // FIXME: catch IllegalArgument 
                this.dateViewStr = // FIXME: use DateTimeFormat
                    "" + (date.getMonth() + 1) + "/" + date.getDate();
            }

            String getField(int index) {
                if (index == 0) {return dateViewStr;}
                return "";
            }
        }

        static class CalRec extends Record {
            final String desc;
            final String quantity;
            final String calPerUnit;
            final String calories;

            CalRec(String dateStr, 
                   String desc, double quantity, double calPerUnit) {
                super(dateStr);
                this.desc = desc;
                this.quantity = "" + quantity;
                this.calPerUnit = "" + calPerUnit;
                this.calories = "" + (quantity * calPerUnit);
            }

            String getField(int index) {
                if (index == 1) {return desc;}
                if (index == 2) {return calories;}
                return super.getField(index);
            }

        }

        static class WeightRec extends Record {
            final String weight;

            WeightRec(String dateStr, double weight) {
                super(dateStr);
                this.weight = "" + weight;
            }

            String getField(int index) {
                if (index == 1) {return weight;}
                return super.getField(index);
            }
        }

        static class DB extends ArrayList implements Comparator {
            public int compare(Object o1, Object o2) {
                return ((Record)o1).date.compareTo(((Record)o2).date);
            }

            public boolean add(Object o) {
                int index = Collections.binarySearch(this, o, this);
                int size = size();
                if (index >= 0) {
                    while (index < size && compare(get(index), o) == 0)
                        index++;
                    add(index, o);
                }
                else {
                    add(-index - 1, o);
                }
                return true;
            }
        }

        final static DB food = new DB();
        final static DB pA = new DB();
        final static DB weight = new DB();

        static {
            food.add(new CalRec("5/7/04", "pasta", 14.0, 20.0));

            pA.add(new CalRec("5/7/04", "walking", 0.5, 300.0));

            weight.add(new WeightRec("1/1/04", 90.0));
            weight.add(new WeightRec("4/2/04", 87.0));
            weight.add(new WeightRec("5/7/04", 84.0));
            weight.add(new WeightRec("4/2/04", 87.1));
            weight.add(new WeightRec("4/2/04", 87.2));

            weight.add(new WeightRec("1/1/04", 90.0));
            weight.add(new WeightRec("4/2/04", 87.0));
            weight.add(new WeightRec("5/7/04", 84.0));
            weight.add(new WeightRec("4/2/04", 87.1));
            weight.add(new WeightRec("4/2/04", 87.2));

            weight.add(new WeightRec("1/1/04", 90.0));
            weight.add(new WeightRec("4/2/04", 87.0));
            weight.add(new WeightRec("5/7/04", 84.0));
            weight.add(new WeightRec("4/2/04", 87.1));
            weight.add(new WeightRec("4/2/04", 87.2));
        }

    }
}
