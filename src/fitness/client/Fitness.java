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
import java.util.ListIterator;
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
    static Main main;
    static Totals totals;
    static DB food;
    static DB pA;
    static DB weight;
    static Options options;
    
    static {
        log("Starting...");
    }

    public void onModuleLoad() {
        totals = new Totals();
        food = new DB(Model.food, new CalRec(c.editFood(), c.foodUnits())); 
        pA = new DB(Model.pA, new CalRec(c.editPA(), c.pAUnits())); 
        weight = new DB(Model.weight, new WeightRec(c.editWeight()));
        options = new Options();

        main = new Main();
        RootPanel.get().add(main);
    }

    static class Main extends TabPanel {
        private int selectedIndex = -1;
        private boolean isModal = false;

        {
            add(Fitness.totals, c.totals());
            add(Fitness.food, c.food());
            add(Fitness.pA, c.pA());
            add(Fitness.weight, c.weight());
            selectTab(0);
        }

        public boolean onBeforeTabSelected(SourcesTabEvents sender,
                                           int tabIndex) {
            if (isModal)
                return false;
            selectedIndex = tabIndex;
            Widget tab = getWidget(tabIndex);
            ((Page)tab).update();
            return super.onBeforeTabSelected(sender, tabIndex);
        }

        void setModal(boolean isModal) {this.isModal = isModal;}

        int getSelectedIndex() {return selectedIndex;}

    };

    static class Input extends Composite 
                    implements ChangeListener, ClickListener {
        private final TextBox tb = new TextBox();
        private final ChangeListener changeL;
        private ClickListener clickL = null;
        private boolean isValid = true;

        protected Input(String text, ChangeListener changeL) {
            this.changeL = changeL;
            tb.addChangeListener(this);
            tb.addClickListener(this);
            initWidget(tb);
            if (text != null)
                change(text);
        }

        protected void setClickListener(ClickListener clickL) {
            this.clickL = clickL;
        }

        protected void setVisibleLength(int len) {
            tb.setVisibleLength(len);
        }

        protected String getText() {
            String t = tb.getText();
            return t;
        }

        protected void setText(String text) {
            tb.setText(text);
        }

        boolean isValid() {
            return isValid;
        }

        void setValid(boolean isValid) {
            this.isValid = isValid;
            if (!isValid) {
                tb.setStyleName("gwt-TextBox-invalid");
            }
            else {
                if (tb.getStyleName().equals("gwt-TextBox-invalid"))
                    tb.setStyleName("gwt-TextBox");
            }
        }

        void setReadOnly(boolean readonly) {
            tb.setReadOnly(readonly);
        }

        void change(String text) {
            setText(text);
            onChange(tb);
        }

        public void onChange(Widget sender) {
            setValid(validate());
            if (isValid && changeL != null && sender != null) {
                changeL.onChange(this);
            }
        }

        public void onClick(Widget sender) {
            if (clickL != null && sender != null) {
                clickL.onClick(this);
            }
        }

        protected boolean validate() {return true;}
    }

    static class DateInput extends Input {
        final static long DAY_IN_MILLIS = 24L * 60L * 60L * 1000L;
        final static DateTimeFormat mformat =
            DateTimeFormat.getMediumDateFormat();
        final static DateTimeFormat sformat = 
            DateTimeFormat.getShortDateFormat();
        private Date date;
        private final Dialog dialog = new Dialog(this);

        public DateInput(String text, ChangeListener cl) {
            super(text, cl);
            setVisibleLength(12);
        }

        public DateInput(ChangeListener cl) {
            this(null, cl);
            set(today());
        }

        protected boolean validate() {
            try {
                String t = getText().trim();
                date = mformat.parse(t);
                // FIXME: check that date is not older than the history
                setText(t);
                return true;
            }
            catch (IllegalArgumentException ile) {
                log(c.badDate()+": '"+getText()+"'");
            }
            return false;
        }

        Date get() {
            return date;
        }

        void set(Date date) {
            this.date = date;
            setText(mformat.format(date));
            setValid(true);
        }

        void set() {
            set(today());
        }

        public void onClick(Widget sender) {
            dialog.init(thisMonth(get()));
            dialog.show();
            super.onClick(sender);
        }

        static class Dialog extends Fitness.Dialog implements TableListener {
            // FIXME: allow setting of first day of the week
            static final DateTimeFormat yearF = 
                DateTimeFormat.getFormat("yyyy");
            static final DateTimeFormat monthF = 
                DateTimeFormat.getFormat("MMMM");
            static final DateTimeFormat dowF = DateTimeFormat.getFormat("E");
            static final String[] DOW = new String[7];
            private final DockPanel dp = new DockPanel();
            private Grid g = new Grid(0, 7);
            private Grid head = new Grid(1, 4);
            private DateInput di;
            private Date month;
            private int nDays;

            static {
                Date day = today();
                day = increment(day, -day.getDay());
                DOW[0] = dowF.format(day);
                for (int d = 1; d < 7; d++) {
                    day = increment(day, 1);
                    DOW[d] = dowF.format(day);
                }
            }

            Dialog(DateInput di) {
                super(c.date(), false);
                this.di = di;
                g.addTableListener(this);
                g.setSize("100%", "100%");
                dp.add(g, CENTER);
                head.setWidth("100%");
                head.addTableListener(this);
                dp.add(head, NORTH);
                setContent(dp);
            }

            void init(Date month) {
                boolean sameMonth = compare(month, thisMonth(di.get())) == 0;
                int dom = di.get().getDate() - 1;
                this.month = month;
                Date nextMonth = thisMonth(increment(month, 45));
                int dow = month.getDay();
                int nWeeks = 
                    (diff(nextMonth, increment(month, -dow)) + 6)/ 7;
                g.resizeRows(nWeeks + 1);
                nDays = diff(nextMonth, month);
                for (int d = 0; d < 7; d++) {
                    g.setText(0, d, DOW[d]);
                }
                for (int row = 1; row <= nWeeks; row++) {
                    for (int col = 0; col < 7; col++) {
                        g.getCellFormatter().setStyleName(
                            row, col, "gwt-DateInput-Dialog");
                        int d = (row - 1) * 7 + col - dow;
                        g.getCellFormatter().setHorizontalAlignment(
                            row, col, ALIGN_CENTER);
                        if (sameMonth && d == dom)
                            g.getCellFormatter().setStyleName(
                                row, col, "gwt-DateInput-Dialog-current");
                        if (d < 0 || d >= nDays) {
                            d = increment(month, d).getDate() - 1;
                            g.getCellFormatter().setStyleName(
                                row, col, "gwt-DateInput-Dialog-anotherMonth");
                        }
                        g.setText(row, col, String.valueOf(d + 1));
                    }
                }
                for (int col = 0; col < 4; col++) {
                    head.getCellFormatter().setHorizontalAlignment(
                        0, col, ALIGN_CENTER);
                }
                head.setText(0, 0, "<<");
                head.setText(0, 1, monthF.format(month));
                head.setText(0, 2, yearF.format(month));
                head.setText(0, 3, ">>");
            }

            public void onCellClicked(SourcesTableEvents sender, 
                                      int row, int cell) {
                if (sender == head) {
                    if (cell > 0 && cell < 3)
                        return;
                    init(thisMonth(increment(month, cell == 0 ? -15 : 45)));
                    return;
                }
                // sender == g
                int d = (row - 1) * 7 + cell - month.getDay();
                if (d < 0 || d >= nDays) {
                    init(thisMonth(increment(month, d < 0 ? -15 : 45)));
                    return;
                }
                di.set(increment(month, d));
                di.change(di.getText());
                dismiss();
            }
        }

        /*
         * The functions below should really be a part of an subclass of Date
         */
        static String small(Date date) {
            return sformat.format(date);
        }

        static Date today(long time) {
            if (time < 0)
                time = System.currentTimeMillis();
            Date today = new Date(time);
            return mformat.parse(mformat.format(today));
        }

        static Date today(Date d) {
            return today(d.getTime());
        }

        static Date today() {
            return today(-1);
        }

        // Assume that the arguments are "whole" days
        static int diff(Date late, Date early) {
            return (int)((late.getTime() - early.getTime() + 
                          DAY_IN_MILLIS / 2) / DAY_IN_MILLIS);
        }

        // Assume that the argument is a "whole" day
        static Date increment(Date d, int n) {
            return today(d.getTime() + n * DAY_IN_MILLIS + DAY_IN_MILLIS / 2);
        }

        static Date previousDay(int n) {
            return increment(today(), -n);
        }

        static Date yesterday() {
            return previousDay(1);
        }

        static Date thisWeek() {
            return previousDay(today().getDay());
        }

        static Date thisMonth(Date d) {
            if (d == null)
                d = today();
            return increment(d, 1 - d.getDate());
        }


        public static int compare(Date d1, Date d2) {
            // Unforturnately, time is not rounded to a full second
            return (int)(d1.getTime() / 1000 - d2.getTime() / 1000);
        }


    }

    static String d2s(double d) {
        long l = (long)(d * 100 + 0.5);
        d = l / 100.0;
        return String.valueOf(d);
    }

    static class DoubleInput extends Input {
        private double d;

        public DoubleInput(String text, ChangeListener cl) {
            super(text, cl);
            if (text == null)
                set(0);
        }

        protected boolean validate() {
            try {
                d = Double.parseDouble(getText());
                return true;
            }
            catch (NumberFormatException nfe) {
                log(c.badDouble()+": '"+getText()+"'");
            }
            return false;
        }

        double get() {
            return d;
        }

        void set(double d) {
            this.d = d;
            setText(d2s(d));
            setValid(true);
        }
    }

    static class IntegerInput extends Input {
        private int i;

        public IntegerInput(String text, ChangeListener cl) {
            super(text, cl);
            if (text == null)
                set(0);
        }

        protected boolean validate() {
            try {
                i = Integer.parseInt(getText());
                return true;
            }
            catch (NumberFormatException nfe) {
                log(c.badInteger()+": '"+getText()+"'");
            }
            return false;
        }

        int get() {
            return i;
        }

        void set(int i) {
            this.i = i;
            setText(String.valueOf(i));
            setValid(true);
        }
    }

    static class Page extends DockPanel {
        FlowPanel buttons = new FlowPanel();

        Page() {
            setSpacing(4);
            add(buttons, SOUTH);
            setCellHorizontalAlignment(buttons, ALIGN_CENTER);
        }

        void setContent(Widget w, boolean fixed) {
            w.setWidth("100%");
            if (fixed) {
                w = new ScrollPanel(w);
                w.setHeight("16em"); //FIXME: should this be in the style sheet?
                w.setWidth("20em"); //FIXME: should this be in the style sheet?
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

    static class Totals extends Page implements ChangeListener {
        final Grid tg;
        final ListBox showFor = new ListBox();
        final ChangeListener dateCL = new ChangeListener() {
            public void onChange(Widget sender) {
                for (int i = 0; i < showFor.getItemCount(); i++) {
                    if (showFor.getItemText(i).equals(c.range())) {
                        showFor.setItemSelected(i, true);
                        break;
                    }
                }
                Totals.this.onChange(showFor);
            }
        };
        final DateInput fromDate = new DateInput(dateCL);
        final DateInput thruDate = new DateInput(dateCL);

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
            vp.add(tg);

            setContent(vp);
            
            addButton(c.options(), new ClickListener() {
                public void onClick(Widget sender) {
                    Fitness.options.show();
                }
            });

            onChange(showFor);
        }

        public void onChange(Widget sender) {
            ListBox lb = (ListBox)sender;
            String showFor = lb.getItemText(lb.getSelectedIndex());
            boolean readonly = true;
            if      (showFor.equals(c.today())) {
                fromDate.set();
                thruDate.set();
            }
            else if (showFor.equals(c.yesterday())) {
                fromDate.set(DateInput.yesterday());
                thruDate.set(DateInput.yesterday());
            }
            else if (showFor.equals(c.thisWeek())) {
                fromDate.set(DateInput.thisWeek());
                thruDate.set(DateInput.yesterday());
            }
            else if (showFor.equals(c.thisMonth())) {
                fromDate.set(DateInput.thisMonth(null));
                thruDate.set(DateInput.yesterday());
            }
            else if (showFor.equals(c.allData())) {
                Date from = ((Model.Record)Model.food.first()).date;
                if (DateInput.compare(
                        ((Model.Record)Model.pA.first()).date, from) < 0) {
                    from = ((Model.Record)Model.pA.first()).date;
                }
                Date thru = ((Model.Record)Model.food.last()).date;
                if (DateInput.compare(
                        thru, ((Model.Record)Model.pA.last()).date) < 0) {
                    thru = ((Model.Record)Model.pA.last()).date;
                }
                fromDate.set(from);
                thruDate.set(thru);
            }
            else if (showFor.equals(c.range())) {
                readonly = false;
            }
            fromDate.setReadOnly(readonly);
            thruDate.setReadOnly(readonly);
            update();
        }

        void update() {
            Model.Totals.update(fromDate.get(), thruDate.get());
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
        int count;

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
            count = mdb.size();
            g.resizeRows(count);
            ListIterator it = mdb.listIterator(count);
            int row = 0;
            while (it.hasPrevious()) {
                Model.Record rec = (Model.Record)it.previous();
                for (int col = 0; col < mdb.nCol; col++) {
                    g.setText(row, col, rec.getField(col));
                }
                row++;
            }
        }

        public void onCellClicked(SourcesTableEvents sender, 
                                  int row, int cell) {
            record.init(count - 1 - row);
            record.show();
        }
    }

    static class Dialog extends Page {
        static private ArrayList pageStack = new ArrayList();

        private String title;

        Dialog(String title, boolean withButtons) {
            this.title = title;
            
            if (withButtons) {
                addButton(c.oK(), new ClickListener() {
                    public void onClick(Widget sender) {
                        accept();
                    }
                });
                addButton(c.cancel(), new ClickListener() {
                    public void onClick(Widget sender) {
                        dismiss();
                    }
                });
            }
        }

        Dialog(String title) {
            this(title, true);
        }

        void accept() {}

        void dismiss() {hide();}

        void show() {
            pageStack.add(0, new Integer(main.getSelectedIndex()));
            main.insert(this, title, 0);
            main.setModal(false);
            main.selectTab(0);
            main.setModal(true);
        }

        void hide() {
            main.setModal(false);
            main.remove(this);
            main.selectTab(((Integer)pageStack.remove(0)).intValue());
        }
    }

    static class Record extends Dialog {
        protected final Grid g = new Grid(2, 2);
        protected final DateInput date = new DateInput(null);
        protected DB db;
        protected Model.DB mdb;
        protected int row = -1;

        Record(String title) {
            super(title);
            g.setText(0, 0, c.date());
            g.setWidget(0, 1, date);
            g.setWidth("100%");
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
            date.set();
            super.dismiss();
        }

        void init(int row) {
            this.row = row;
            date.set(((Model.Record)mdb.get(row)).date);
        }
    }

    static class CalRec extends Record {
        private final TextBox desc = new TextBox();
        private final DoubleInput quantity = new DoubleInput(null, null);
        private final ListBox unit = new ListBox();
        private final DoubleInput calPerUnit = new DoubleInput(null, null);

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
            if (!date.isValid() || !quantity.isValid() || !calPerUnit.isValid())
                return;
            Model.CalRec mcr = 
                new Model.CalRec(date.get(), desc.getText(), 
                                 quantity.get(), calPerUnit.get());
            apply(mcr);
        }

        void dismiss() {
            desc.setText("");
            quantity.set(0);
            // FIXME: reset unit
            calPerUnit.set(0);
            super.dismiss();
        }

        void init(int row) {
            super.init(row);
            Model.CalRec mcr = (Model.CalRec)mdb.get(row);
            desc.setText(mcr.desc);
            quantity.change(mcr.quantity);
            //FIXME: set unit (need to save it first in the model)
            calPerUnit.change(mcr.calPerUnit);
        }
    }

    static class WeightRec extends Record {
        private final DoubleInput weight = new DoubleInput(null, null);

        WeightRec(String title) {
            super(title);
            g.setText(1, 0, c.weightRec());
            g.setWidget(1, 1, weight);
        }

        void accept() {
            if (!date.isValid() || !weight.isValid())
                return;
            Model.WeightRec mwr = 
                new Model.WeightRec(date.get(), weight.get());
            apply(mwr);
        }

        void dismiss() {
            weight.set(0);
            super.dismiss();
        }

        void init(int row) {
            super.init(row);
            weight.change(((Model.WeightRec)mdb.get(row)).getField(1));
        }
    }

    static class Options extends Dialog {
        DoubleInput metabolism;
        DoubleInput goalWeight;
        RadioButton weightIsPounds;
        RadioButton weightIsKilograms;
        IntegerInput history;

        Options() {
            super(c.options());

            VerticalPanel vp = new VerticalPanel();
            Grid g = new Grid(5, 2);

            g.setText(0, 0, c.metabolismOpt());
            metabolism = new DoubleInput(d2s(Model.Options.metabolism), null);
            g.setWidget(0, 1, metabolism);

            g.setText(1, 0, c.goalWeight());
            goalWeight = new DoubleInput(d2s(Model.Options.goalWeight), null);
            g.setWidget(1, 1, goalWeight);

            g.setText(2, 0, c.weightOpt());
            FlowPanel p = new FlowPanel();
            weightIsPounds = new RadioButton(c.weightOpt(), c.pounds());
            p.add(weightIsPounds);
            weightIsPounds.setChecked(Model.Options.weightIsPounds);
            weightIsKilograms = new RadioButton(c.weightOpt(), c.kilograms());
            p.add(weightIsKilograms);
            weightIsKilograms.setChecked(Model.Options.weightIsKilograms);
            g.setWidget(2, 1, p);

            g.setText(3, 0, c.historyDays());
            history = 
                new IntegerInput(String.valueOf(Model.Options.history), null);
            g.setWidget(3, 1, history);

            CheckBox cb = new CheckBox(c.console());
            cb.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    if (((CheckBox)sender).isChecked()) {Console.show();}
                    else {Console.hide();}
                }
            });
            g.setWidget(4, 1, cb);

            g.setWidth("100%");
            vp.add(g);

            setContent(vp);
        }

        void accept() {
            if (!metabolism.isValid() ||
                !goalWeight.isValid() ||
                !history.isValid())
                return;
            Model.Options.update(
                    metabolism.get(),
                    goalWeight.get(),
                    weightIsPounds.isChecked(), weightIsKilograms.isChecked(),
                    history.get());
            totals.update();
            dismiss();
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
                double pACalories = 0.0;
                for (Iterator it = pA.range(); it.hasNext();) {
                    CalRec cr = (CalRec)it.next();
                    pACalories += cr.calories;
                }
                int daysInRange = DateInput.diff(thruDate, fromDate) + 1;
                Totals.daysInRange = String.valueOf(daysInRange);
                Totals.caloriesIn = d2s(caloriesIn / daysInRange);
                Totals.pACalories = d2s(pACalories / daysInRange);
                double metabolism = Options.metabolism *
                    (weight.size() > 0 ? ((WeightRec)weight.last()).weight : 0);
                Totals.metabolism = d2s(metabolism);
                netCalories = d2s((caloriesIn - pACalories - metabolism));
                double behavioralWeight = (caloriesIn - pACalories) / 
                    Options.metabolism / daysInRange;
                if (Options.weightIsKilograms)
                    behavioralWeight /= POUNDS_PER_KILO;
                Totals.behavioralWeight = d2s(behavioralWeight);
                double calsLeftToEat = Options.goalWeight * Options.metabolism;
                if (Options.weightIsKilograms)
                    calsLeftToEat *= POUNDS_PER_KILO;
                calsLeftToEat += (pACalories - caloriesIn) / daysInRange;
                Totals.calsLeftToEat = d2s(calsLeftToEat);
            }
        }

        static class Options {
            static double metabolism = 10.0;
            static double goalWeight = 0.0;
            static boolean weightIsPounds = false;
            static boolean weightIsKilograms = true;
            static int history = 90;

            static void update(
                double metabolism,
                double goalWeight,
                boolean weightIsPounds, boolean weightIsKilograms,
                int history) {

                Options.metabolism = metabolism;
                Options.goalWeight = goalWeight;
                Options.weightIsPounds = weightIsPounds;
                Options.weightIsKilograms = weightIsKilograms;
                Options.history = history;
                food.truncate();
                pA.truncate();
            }
        }

        // Records
        static class Record {
            final Date date;
            final String dateViewStr;

            Record(Date date) {
                this.date = date;
                this.dateViewStr = DateInput.small(date);
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

            WeightRec(Date date, double weight) {
                super(date);
                if (Options.weightIsKilograms)
                    weight *= POUNDS_PER_KILO;
                this.weight = weight;
            }

            String getField(int index) {
                if (index == 1) {
                    return d2s(weight / (Options.weightIsKilograms ? 
                                         POUNDS_PER_KILO : 1));
                }
                return super.getField(index);
            }
        }

        static class DB implements Comparator {
            private final ArrayList al = new ArrayList();
            final int nCol;

            DB(int nCol) {
                this.nCol = nCol;
            }

            int size() {return al.size();}

            Object get(int index) {return al.get(index);}

            void set(int index, Object o) {al.set(index, o);}

            int search(Date d) {
                Record r = new Record(d);
                return search(r);
            }

            int search(Object o) {
                return Collections.binarySearch(al, o, this);
            }

            public boolean add(Object o) {
                int index = search(o);
                int size = size();
                if (index >= 0) {
                    while (index < size && compare(get(index), o) == 0)
                        index++;
                    al.add(index, o);
                }
                else {
                    al.add(-index - 1, o);
                }
                return true;
            }

            public int compare(Object o1, Object o2) {
                return DateInput.compare(((Record)o1).date, ((Record)o2).date);
            }

            Object first() {return get(0);}

            Object last() {return get(size() - 1);}

            ListIterator listIterator(int index) {
                return al.listIterator(index);
            }

            void truncate() {
                Date firstDate = DateInput.previousDay(Options.history - 1);
                Iterator it = al.iterator();
                while (it.hasNext()) {
                    Record r = (Record)it.next();
                    if (DateInput.compare(firstDate, r.date) <= 0)
                        break;
                    it.remove();
                }
            }

            Iterator range() {
                if (this != weight)
                    truncate();
                return new Range(this, Totals.fromDate, Totals.thruDate);
            }

            static class Range implements Iterator {
                private DB db;
                private Date thruDate;
                private int firstIndex;
                private Iterator iter;
                private Record next;

                Range(DB db, Date fromDate, Date thruDate) {
                    this.db = db;
                    this.thruDate = thruDate;
                    int index = 0;
                    if (fromDate != null) {
                        index = db.search(fromDate);
                        if (index >= 0) {
                            while (index > 0 && 
                                    DateInput.compare(
                                        ((Record)db.get(index - 1)).date, 
                                        thruDate) == 0) {
                                index--;
                            }
                        }
                        else {
                            index = -index - 1;
                        }
                    }
                    firstIndex = index;
                    iter = db.al.listIterator(index);
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
                        if (thruDate != null && 
                            DateInput.compare(next.date, thruDate) > 0) {
                            next = null;
                        }
                    }
                    return curr;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }

            }
        }


        final static DB food = new DB(3);
        final static DB pA = new DB(3);
        final static DB weight = new DB(2);

        // Testing data
        static {
            Options.metabolism = 11.0;
            Options.goalWeight = 77.0;

            food.add(new CalRec(DateInput.today(), "pasta", 14.0, 20.0));
            food.add(new CalRec(DateInput.previousDay(50), "pasta", 18.0, 20.0));
            food.add(new CalRec(DateInput.previousDay(95), "pasta", 28.0, 20.0));

            pA.add(new CalRec(DateInput.today(), "walking", 0.5, 300.0));

            weight.add(new WeightRec(DateInput.previousDay(50), 90.0));
            weight.add(new WeightRec(DateInput.previousDay(34), 87.0));
            weight.add(new WeightRec(DateInput.previousDay(12), 84.0));
            weight.add(new WeightRec(DateInput.previousDay(2), 82.0));

        }

    }

    static void log(Object message) {
        System.err.println(message);
        Console.println(message.toString());
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

