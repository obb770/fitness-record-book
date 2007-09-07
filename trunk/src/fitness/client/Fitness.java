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
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import com.google.gwt.i18n.client.DateTimeFormat;

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
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.RootPanel;

public class Fitness implements EntryPoint {

    static Constants c = (Constants)GWT.create(Constants.class);
    static TabPanel tp;
    static Totals totals;
    static DB food;
    static DB pA;
    static DB weight;
    static Options options;
    
    public void onModuleLoad() {
        totals = new Totals();
        food = new DB(Model.food, new CalRec(c.editFood(), c.foodUnits())); 
        pA = new DB(Model.pA, new CalRec(c.editPA(), c.pAUnits())); 
        weight = new DB(Model.weight, new WeightRec(c.editWeight()));
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

    static String d2s(double d) {
        long l = (long)(d * 100 + 0.5);
        d = l / 100.0;
        return String.valueOf(d);
    }

    static void mark(TextBox tb) {
        tb.setText("*"+tb.getText()+"*");
    }

    static String unmark(TextBox tb) {
        String t = tb.getText();
        if (t.startsWith("*") && t.endsWith("*")) {
            t = t.substring(1, t.length() - 1);
            tb.setText(t);
        }
        return t;
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
        final static long DAY_IN_MILLIS = 24L * 60L * 60L * 1000L;
        final static DateTimeFormat sformat = 
            DateTimeFormat.getShortDateFormat();
        final static DateTimeFormat mformat = 
            DateTimeFormat.getMediumDateFormat();
        final TextBox tb = new TextBox();
        private Date date;

        DateView() {
            tb.setVisibleLength(12);
            tb.addChangeListener(this);
            setDate();
            initWidget(tb);
        }

        Date getDate() {return date;}

        void setDate(Date date) {
            this.date = date;
            tb.setText(DateView.medium(date));
        }

        void setDate() {setDate(today());}

        void setReadOnly(boolean readonly) {tb.setReadOnly(readonly);}


        // FIXME: must update the parent totals page (add callback?) !!!!
        public void onChange(Widget sender) { 
                                    // FIXME: catch IllegalArgumentException {
            TextBox tb = (TextBox)sender;
            try {
                date = mformat.parse(unmark(tb));
            }
            catch (IllegalArgumentException ile) {
                log(c.badDate());
                mark(tb);
                //setDate(date);
            }
        }

        static String small(Date date) {
            return sformat.format(date);
        }

        static String medium(Date date) {
            return mformat.format(date);
        }

        static Date today(long time) { // FIXME: too many Date instances?
            if (time < 0)
                time = System.currentTimeMillis();
            Date today = new Date(time);
            return mformat.parse(mformat.format(today));
        }

        static Date today() {
            return today(-1);
        }

        // The date n days ago
        static Date dayBefore(int n) {
            return today(today().getTime() - n * DAY_IN_MILLIS);
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
            fromDate.setReadOnly(true);
            thruDate.setReadOnly(true);
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
            if      (showFor.equals(c.today())) {
                fromDate.setDate();
                thruDate.setDate();
            }
            else if (showFor.equals(c.yesterday())) {
                fromDate.setDate(DateView.yesterday());
                thruDate.setDate(DateView.yesterday());
            }
            else if (showFor.equals(c.thisWeek())) {
                fromDate.setDate(DateView.thisWeek());
                thruDate.setDate(DateView.yesterday());
            }
            else if (showFor.equals(c.thisMonth())) {
                fromDate.setDate(DateView.thisMonth());
                thruDate.setDate(DateView.yesterday());
            }
            else if (showFor.equals(c.allData())) {
                fromDate.setDate(DateView.today(0L));
                thruDate.setDate(DateView.today(0x7fffffff * 1000L));
            }
            else if (showFor.equals(c.range())) {
                fromDate.setDate(DateView.today());
                thruDate.setDate(DateView.today());
                fromDate.setReadOnly(false);
                thruDate.setReadOnly(false);
            }
            update();
        }

        void update() {
            Model.Totals.update(fromDate.getDate(), thruDate.getDate());
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
        final Record record;
        final Grid g;

        DB(Model.DB mdb, Record record) { 
            this.mdb = mdb;
            this.record = record;
            record.setDB(this);
            record.setModelDB(mdb);
            g = new Grid(0, mdb.nCol);
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
            int row = 0;
            for (Iterator it = mdb.range(); it.hasNext(); it.next(), row++);
            g.resizeRows(row);
            row = 0;
            for (Iterator it = mdb.range(); it.hasNext(); row++) {
                Model.Record r = (Model.Record)it.next();
                for (int j = 0; j < mdb.nCol; j++) {
                    g.setText(row, j, r.getField(j));
                }
            }
        }

        public void onCellClicked(SourcesTableEvents sender, 
                                  int row, int cell) {
            record.init(row + mdb.firstIndex());
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
        protected final DateView date = new DateView();
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
            date.setDate();
            super.dismiss();
        }

        void init(int row) {
            this.row = row;
            date.setDate(((Model.Record)mdb.get(row)).date);
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
                    new Model.CalRec(date.getDate(), desc.getText(), 
                                     Double.parseDouble(quantity.getText()),
                                     Double.parseDouble(calPerUnit.getText()));
                apply(mcr);
            }
            catch (NumberFormatException nfe) {
                log(c.badQuantityOrCalPerUnit());
            }
            catch (IllegalArgumentException iae) {
                log(c.badDate());
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
            quantity.setText(mcr.quantity);
            //FIXME: set unit (need to save it first in the model)
            calPerUnit.setText(mcr.calPerUnit);
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
                    new Model.WeightRec(date.getDate(), 
                                        Double.parseDouble(weight.getText()));
                apply(mwr);
            }
            catch (NumberFormatException nfe) {
                log(c.badWeight());
            }
            catch (IllegalArgumentException iae) {
                log(c.badDate());
            }
        }

        void dismiss() {
            weight.setText("");
            super.dismiss();
        }

        void init(int row) {
            super.init(row);
            weight.setText(((Model.WeightRec)mdb.get(row)).weightStr);
        }
    }

    static class Options extends Dialog implements ClickListener {
        TextBox metabolism;
        RadioButton goalIsWeight;
        RadioButton goalIsDeficit;
        TextBox goalWeight;
        TextBox goalDeficit;
        RadioButton weightIsPounds;
        RadioButton weightIsKilograms;
        TextBox history;

        Options() {
            super(c.options());

            VerticalPanel vp = new VerticalPanel();
            Grid g = new Grid(7, 2);

            g.setText(0, 0, c.metabolismOpt());
            metabolism = new TextBox();
            metabolism.setText(d2s(Model.Options.metabolism));
            g.setWidget(0, 1, metabolism);

            g.setText(1, 0, c.goal());
            FlowPanel p = new FlowPanel();
            goalIsWeight = new RadioButton(c.goal(), c.weightOptVal());
            goalIsWeight.addClickListener(this);
            p.add(goalIsWeight);
            goalIsWeight.setChecked(Model.Options.goalIsWeight);
            goalIsDeficit = new RadioButton(c.goal(), c.deficit());
            goalIsDeficit.addClickListener(this);
            p.add(goalIsDeficit);
            goalIsDeficit.setChecked(Model.Options.goalIsDeficit);
            g.setWidget(1, 1, p);

            g.setText(2, 0, c.goalWeight());
            goalWeight = new TextBox();
            goalWeight.setText(d2s(Model.Options.goalWeight));
            g.setWidget(2, 1, goalWeight);

            g.setText(3, 0, c.goalDeficit());
            goalDeficit = new TextBox();
            goalDeficit.setText(d2s(Model.Options.goalDeficit));
            g.setWidget(3, 1, goalDeficit);

            g.setText(4, 0, c.weightOpt());
            p = new FlowPanel();
            weightIsPounds = new RadioButton(c.weightOpt(), c.pounds());
            p.add(weightIsPounds);
            weightIsPounds.setChecked(Model.Options.weightIsPounds);
            weightIsKilograms = new RadioButton(c.weightOpt(), c.kilograms());
            p.add(weightIsKilograms);
            weightIsKilograms.setChecked(Model.Options.weightIsKilograms);
            g.setWidget(4, 1, p);

            g.setText(5, 0, c.historyDays());
            history = new TextBox();
            history.setText(String.valueOf(Model.Options.history));
            g.setWidget(5, 1, history);

            CheckBox cb = new CheckBox(c.console());
            cb.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    if (((CheckBox)sender).isChecked()) {Console.show();}
                    else {Console.hide();}
                }
            });
            g.setWidget(6, 1, cb);

            g.setWidth("100%");
            vp.add(g);

            setContent(vp);

            onClick(null);
        }

        public void onClick(Widget sender) {
            goalWeight.setReadOnly(!goalIsWeight.isChecked());
            goalDeficit.setReadOnly(!goalIsDeficit.isChecked());
        }

        void accept() {
            try {
                Model.Options.update(
                    Double.parseDouble(metabolism.getText()),
                    goalIsWeight.isChecked(), goalIsDeficit.isChecked(),
                    Double.parseDouble(goalWeight.getText()),
                    Double.parseDouble(goalDeficit.getText()),
                    weightIsPounds.isChecked(), weightIsKilograms.isChecked(),
                    Integer.parseInt(history.getText()));
                totals.update();
                dismiss();
            }
            catch (NumberFormatException nfe) {
                log(c.badOption());
            }
        }
    }

    static class Model {
        final static double POUNDS_PER_KILO = 2.2046;

        static class Totals {
            static Date fromDate;
            static Date thruDate;

            static String caloriesIn;
            static String pACalories;
            static String metabolism;
            static String netCalories;
            static String behavioralWeight;
            static String daysInRange;
            static String calsLeftToEat;

            static void update(Date fromDate, Date thruDate) {
                Totals.fromDate = fromDate;
                Totals.thruDate = thruDate;

                double caloriesIn = 0.0;
                for (Iterator it = food.range(); it.hasNext();) {
                    CalRec cr = (CalRec)it.next();
                    caloriesIn += cr.calories;
                }
                Totals.caloriesIn = d2s(caloriesIn);
                double pACalories = 0.0;
                for (Iterator it = pA.range(); it.hasNext();) {
                    CalRec cr = (CalRec)it.next();
                    pACalories += cr.calories;
                }
                Totals.pACalories = d2s(pACalories);
                long daysInRange = (thruDate.getTime() - fromDate.getTime() + 
                    DateView.DAY_IN_MILLIS / 2) / DateView.DAY_IN_MILLIS + 1L;
                Totals.daysInRange = String.valueOf(daysInRange);
                double metabolism = daysInRange * 
                    ((WeightRec)weight.last()).weight * Options.metabolism;
                Totals.metabolism = d2s(metabolism);
                netCalories = d2s(caloriesIn - pACalories - metabolism);
                double behavioralWeight = (caloriesIn - pACalories) / 
                    Options.metabolism / daysInRange;
                if (Options.weightIsKilograms)
                    behavioralWeight /= POUNDS_PER_KILO;
                Totals.behavioralWeight = d2s(behavioralWeight);
                if (Options.goalIsWeight) {
                    double calsLeftToEat = 
                        Options.goalWeight * Options.metabolism;
                    if (Options.weightIsKilograms)
                        calsLeftToEat *= POUNDS_PER_KILO;
                    calsLeftToEat += (pACalories - caloriesIn) / daysInRange;
                    Totals.calsLeftToEat = d2s(calsLeftToEat);
                }
                else {
                    Totals.calsLeftToEat = "N/A"; // FIXME: implement
                }
            }
        }

        static class Options {
            static double metabolism = 10.0;
            static boolean goalIsWeight = true;
            static boolean goalIsDeficit = false;
            static double goalWeight = 0.0;
            static double goalDeficit = 0.0;
            static boolean weightIsPounds = false;
            static boolean weightIsKilograms = true;
            static int history = 90;

            static void update(
                double metabolism,
                boolean goalIsWeight, boolean goalIsDeficit,
                double goalWeight,
                double goalDeficit,
                boolean weightIsPounds, boolean weightIsKilograms,
                int history) {

                Options.metabolism = metabolism;
                Options.goalIsWeight = goalIsWeight;
                Options.goalIsDeficit = goalIsDeficit;
                Options.goalWeight = goalWeight;
                Options.goalDeficit = goalDeficit;
                Options.weightIsPounds = weightIsPounds;
                Options.weightIsKilograms = weightIsKilograms;
                Options.history = history;
            }
        }

        // Records
        static class Record {
            final Date date;
            final String dateViewStr;

            Record(Date date) {
                this.date = date;
                this.dateViewStr = DateView.small(date);
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
            final double calories;
            final String caloriesStr;

            CalRec(Date date, String desc, double quantity, double calPerUnit) {
                super(date);
                this.desc = desc;
                this.quantity = d2s(quantity);
                this.calPerUnit = d2s(calPerUnit);
                this.calories = quantity * calPerUnit;
                this.caloriesStr = d2s(calories);
            }

            String getField(int index) {
                if (index == 1) {return desc;}
                if (index == 2) {return caloriesStr;}
                return super.getField(index);
            }

        }

        static class WeightRec extends Record {
            final double weight;
            final String weightStr;

            WeightRec(Date date, double weight) {
                super(date);
                weightStr = d2s(weight);
                if (Options.weightIsKilograms)
                    weight *= POUNDS_PER_KILO;
                this.weight = weight;
            }

            String getField(int index) {
                if (index == 1) {return weightStr;}
                return super.getField(index);
            }
        }

        static class DB extends ArrayList implements Comparator {
            final int nCol;
            final boolean useRange;

            DB(int nCol, boolean useRange) {
                this.nCol = nCol;
                this.useRange = useRange;
            }

            int search(Object o) {
                return Collections.binarySearch(this, o, this);
            }

            public int compare(Object o1, Object o2) {
                // Unforturnately, time is not rounded to a full second
                return (int)(((Record)o1).date.getTime() / 1000 -
                             ((Record)o2).date.getTime() / 1000);
            }

            public boolean add(Object o) {
                int index = search(o);
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

            Object last() {return get(size() - 1);}

            Iterator range() {
                return useRange ? 
                    new Range(this, Totals.fromDate, Totals.thruDate) :
                    iterator();
            }

            int firstIndex() {
                return useRange ?
                    new Range(this, Totals.fromDate, 
                                    Totals.thruDate).firstIndex() :
                    0;
            }

            static class Range implements Iterator {
                private DB db;
                private Record thru;
                private int firstIndex;
                private Iterator iter;
                private Record next;

                Range(DB db, Date fromDate, Date thruDate) {
                    this.db = db;
                    Record dummy = new Record(fromDate);
                    thru = new Record(thruDate);
                    int index = db.search(dummy);
                    if (index >= 0) {
                        while (index > 0 && 
                               db.compare(db.get(index - 1), dummy) == 0)
                            index--;
                    }
                    else {
                        index = -index - 1;

                    }
                    firstIndex = index;
                    iter = db.listIterator(index);
                    next = null;
                    next();
                }

                int firstIndex() {return firstIndex;}

                public boolean hasNext() {
                    return next != null;
                }

                public Object next() {
                    Record curr = next;
                    next = null;
                    if (iter.hasNext()) {
                        next = (Record)iter.next();
                        if (db.compare(next, thru) > 0)
                            next = null;
                    }
                    return curr;
                }

                public void remove() {}

            }
        }

        final static DB food = new DB(3, true);
        final static DB pA = new DB(3, true);
        final static DB weight = new DB(2, false);

        // Testing data
        static {
            Options.metabolism = 11.0;
            Options.goalWeight = 77.0;

            food.add(new CalRec(DateView.today(), "pasta", 14.0, 20.0));

            pA.add(new CalRec(DateView.today(), "walking", 0.5, 300.0));

            weight.add(new WeightRec(DateView.dayBefore(50), 90.0));
            weight.add(new WeightRec(DateView.dayBefore(34), 87.0));
            weight.add(new WeightRec(DateView.dayBefore(12), 84.0));
            weight.add(new WeightRec(DateView.dayBefore(2), 82.0));

        }

    }

    static void log(String message) {
        System.err.println(message);
        Console.println(message);
    }


    static class Console {
        static boolean initialized = false;
        static DialogBox d;
        static TextArea ta;
        static StringBuffer sb;

        static void init() {
            if (initialized)
                return;
            initialized = true;
            d = new DialogBox(false, false);
            ta = new TextArea();
            ta.setCharacterWidth(80);
            ta.setVisibleLines(24);
            ta.setReadOnly(true);
            d.setText(c.console());
            d.setWidget(ta);
            int left = RootPanel.get().getAbsoluteLeft();
            int bottom = 350;
            d.setPopupPosition(left, bottom);
            sb = new StringBuffer();
        }

        static void println(String message) {
            if (!initialized)
                return;
            sb.append(message);
            sb.append("\n");
            ta.setText(sb.toString());
            ta.setCursorPos(sb.length());
        }

        static void show() {
            init(); 
            d.show();
        }

        static void hide() {
            if (d != null)
                d.hide();
            initialized = false;
            d = null; 
            ta = null; 
            sb = null;
        }
    }

}

