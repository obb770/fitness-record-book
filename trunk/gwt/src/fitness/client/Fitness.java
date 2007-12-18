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
import java.util.Collection;
import java.util.HashSet;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TabListener;
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
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestionHandler;
import com.google.gwt.user.client.ui.SuggestionEvent;
import com.google.gwt.user.client.ui.RootPanel;

public class Fitness implements EntryPoint {

    static Constants c = (Constants)GWT.create(Constants.class);
    static Messages m = (Messages)GWT.create(Messages.class);
    static Main main;
    static Totals totals;
    static DB food;
    static DB pA;
    static DB weight;
    static Options options;
    
    static {
        log(m.starting());
    }

    public void onModuleLoad() {
        // Console.show();

        totals = new Totals();
        food = new DB(c.food(), Model.food, new CalRec(c.editFood()));
        pA = new DB(c.pA(), Model.pA, new CalRec(c.editPA()));
        weight = new DB(c.weight(), 
                        Model.weight, new WeightRec(c.editWeight()));
        
        Page[] pages = {totals, food, pA, weight};
        main = new Main(pages);

        options = new Options();

        RootPanel.get().add(main);
    }

    static class Main extends Composite implements TabListener {
        private TabPanel tp = new TabPanel();
        private ArrayList state = new ArrayList();
        private int size = 0;
        private int selectedIndex = 0;

        Main(Page[] pages) {
            for (int i = 0; i < pages.length; i++) {
                add(pages[i], false);
            }
            tp.selectTab(0);
            tp.addTabListener(this);
            initWidget(tp);
        }

        void add(Page p, boolean isDialog) {
            state.add(p);
            if (isDialog) {
                tp.insert(p, p.title, 0);
                tp.selectTab(0);
                return;
            }
            tp.add(p, p.title);
            size++;
        }

        public void onTabSelected(SourcesTabEvents sender, int tabIndex) {}

        public boolean onBeforeTabSelected(SourcesTabEvents sender,
                                           int tabIndex) {
            if (state.size() == size) {
                selectedIndex = tabIndex;
                Widget tab = tp.getWidget(tabIndex);
                ((Page)tab).update();
            }
            return true;
        }

        void show(Dialog p) {
            int c = tp.getWidgetCount();
            while (c-- > 0)
                tp.remove(0);
            add(p, true);
        }

        void hide() {
            tp.remove(0);
            state.remove(state.size() - 1);
            if (state.size() > size) {
                Page p = (Page)state.remove(state.size() - 1);
                add(p, true);
                return;
            }
            for (Iterator it = state.iterator(); it.hasNext();) {
                Page p = (Page)it.next();
                tp.add(p, p.title);
            }
            tp.selectTab(selectedIndex);
        }

    }

    static class Input extends Composite 
                    implements ChangeListener, ClickListener {
        private final TextBox tb = new TextBox();
        private final String name;
        private final ChangeListener changeL;
        private ClickListener clickL = null;
        private boolean isValid = true;

        protected Input(String name, String text, ChangeListener changeL) {
            this.name = name;
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

        protected String getName() {
            return name;
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

        void setFocus(boolean hasFocus) {
            tb.setFocus(hasFocus);
            tb.setSelectionRange(0, getText().length());
        }

        void change(String text) {
            setText(text);
            onChange(tb);
        }

        public void onChange(Widget sender) {
            setText(getText().trim());
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

        protected void badInput() {
            log(m.badInput(getName(), getText()));
        }
    }

    static class DateInput extends Input {
        final static long DAY_IN_MILLIS = 24L * 60L * 60L * 1000L;
        private final static DateTimeFormat mformat =
            DateTimeFormat.getMediumDateFormat();
        private final static DateTimeFormat sformat = 
            DateTimeFormat.getShortDateFormat();
        private Date date;
        private final Dialog dialog = new Dialog(this);

        DateInput(String name, String text, ChangeListener cl) {
            super(name, text, cl);
            setVisibleLength(12);
        }

        DateInput(String name, ChangeListener cl) {
            this(name, null, cl);
            set(today());
        }

        protected boolean validate() {
            try {
                String t = getText();
                date = mformat.parse(t);
                setText(t);
                return true;
            }
            catch (IllegalArgumentException ile) {
                badInput();
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
                super(c.date());
                addButton(createCancel());
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
                        g.setText(row, col, IntegerInput.format(d + 1));
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

    static class DoubleInput extends Input {
        private final static NumberFormat format = 
            NumberFormat.getFormat("#####0.0#");
        private double d;

        DoubleInput(String name, String text, ChangeListener cl) {
            super(name, text, cl);
            if (text == null)
                set(0);
        }

        DoubleInput(String name, double d, ChangeListener cl) {
            this(name, format(d), cl);
        }

        protected boolean validate() {
            try {
                d = format.parse(getText());
                return true;
            }
            catch (NumberFormatException nfe) {
                badInput();
            }
            return false;
        }

        double get() {
            return d;
        }

        void set(double d) {
            this.d = d;
            setText(format(d));
            setValid(true);
        }

        static String format(double d) {
            if (d >= 1e6)
                d = 999999.99;
            else if (d <= -1e6)
                d = -999999.99;
            return format.format(d);
        }
    }

    static class IntegerInput extends Input {
        private final static NumberFormat format = NumberFormat.getFormat("#");
        private int i;

        IntegerInput(String name, String text, ChangeListener cl) {
            super(name, text, cl);
            if (text == null)
                set(0);
        }

        IntegerInput(String name, int i, ChangeListener cl) {
            this(name, format(i), cl);
        }

        protected boolean validate() {
            try {
                double d = format.parse(getText());
                if ((int)d != d)
                    throw new NumberFormatException();
                return true;
            }
            catch (NumberFormatException nfe) {
                badInput();
            }
            return false;
        }

        int get() {
            return i;
        }

        void set(int i) {
            this.i = i;
            setText(format(i));
            setValid(true);
        }

        static String format(int i) {
            return format.format(i);
        }
    }

    static class Page extends DockPanel {
        final String title;
        final FlowPanel buttons = new FlowPanel();

        Page(String title) {
            this.title = title;
            setSpacing(4);
            add(buttons, SOUTH);
            setCellHorizontalAlignment(buttons, ALIGN_CENTER);
        }

        void setContent(Widget w, boolean fixed) {
            w.setWidth("100%");
            if (fixed) {
                w = new ScrollPanel(w);
                    //FIXME: should these sizes be in the style sheet?
                w.setHeight("16em");
                w.setWidth("20em");
            }
            add(w, CENTER);
        }

        void setContent(Widget w) {setContent(w, true);}

        static Button createButton(String name, ClickListener cl) {
            Button b = new Button(name);
            b.addClickListener(cl);
            return b;
        }

        void addButton(Button b) {buttons.add(b);}

        void clearButtons() {buttons.clear();}

        void update() {}
    }

    static void setListBox(ListBox lb, String text) {
        lb.setItemSelected(lb.getSelectedIndex(), false);
        if (text != null) {
            for (int i = 0; i < lb.getItemCount(); i++) {
                if (lb.getItemText(i).equals(text)) {
                    lb.setItemSelected(i, true);
                    break;
                }
            }
        }
    }

    static String getListBox(ListBox lb) {
        return lb.getItemText(lb.getSelectedIndex());
    }

    static class Totals extends Page implements ChangeListener {
        final Grid tg;
        final ListBox showFor = new ListBox();
        final ChangeListener dateCL = new ChangeListener() {
            public void onChange(Widget sender) {
                setListBox(showFor, c.range());
                Totals.this.onChange(showFor);
            }
        };
        final DateInput fromDate = new DateInput(c.fromDate(), dateCL);
        final DateInput thruDate = new DateInput(c.thruDate(), dateCL);

        Totals() {
            super(c.totals());

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
            
            Button b = createButton(c.options(), new ClickListener() {
                public void onClick(Widget sender) {
                    Fitness.options.show();
                }
            });
            addButton(b);

            onChange(showFor);
        }

        public void onChange(Widget sender) {
            ListBox lb = (ListBox)sender;
            String showFor = getListBox(lb);
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
                thruDate.set(DateInput.today());
            }
            else if (showFor.equals(c.thisMonth())) {
                fromDate.set(DateInput.thisMonth(null));
                thruDate.set(DateInput.today());
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
            Date from = fromDate.get();
            Date thru = thruDate.get();
            if (DateInput.diff(DateInput.today(), from) > 
                Model.Options.history - 1)
                from = DateInput.previousDay(Model.Options.history - 1);
            if (DateInput.diff(DateInput.today(), thru) > 
                Model.Options.history - 1)
                thru = DateInput.previousDay(Model.Options.history - 1);
            if (DateInput.compare(DateInput.today(), thru) < 0)
                thru = DateInput.today();
            if (DateInput.compare(from, thru) > 0)
                thru = from;
            fromDate.set(from);
            thruDate.set(thru);
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

        DB(String title, Model.DB mdb, Record record) { 
            super(title);
            this.mdb = mdb;
            this.record = record;
            record.setDB(this);
            record.setModelDB(mdb);
            g = new Grid(0, mdb.nCol);
            g.setWidth("100%");
            g.addTableListener(this);
            setContent(g);

            Button b = createButton(c.newButton(), new ClickListener() {
                public void onClick(Widget sender) {
                    DB.this.record.show(true);
                }
            });
            addButton(b);
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
            record.show(false);
        }
    }

    static class Dialog extends Page {
        private String title;

        Dialog(String title) {
            super(title);
            
        }

        Button createOK() {
            return createButton(c.oK(), new ClickListener() {
                public void onClick(Widget sender) {
                    accept();
                }
            });
        }

        Button createCancel() {
            return createButton(c.cancel(), new ClickListener() {
                public void onClick(Widget sender) {
                    dismiss();
                }
            });
        }

        void accept() {}

        void dismiss() {hide();}

        void show() {
            main.show(this);
        }

        void hide() {
            main.hide();
        }
    }

    static class Record extends Dialog {
        protected final Grid g = new Grid(2, 2);
        protected final DateInput date = new DateInput(c.date(), null);
        protected DB db;
        protected Model.DB mdb;
        protected int row = -1;
        protected final Button oKB = createOK();
        protected final Button delB = createDel();
        protected final Button cancelB = createCancel();

        Record(String title) {
            super(title);
            g.setText(0, 0, c.date());
            g.setWidget(0, 1, date);
            g.setWidth("100%");
            setContent(g);
        }


        Button createDel() {
            return createButton(c.del(), new ClickListener() {
                public void onClick(Widget sender) {
                    if (row >= 0)
                        mdb.remove(row);
                    db.update();
                    dismiss();
                }
            });
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

        void show(boolean isNew) {
            clearButtons();
            addButton(oKB);
            if (!isNew)
                addButton(delB);
            addButton(cancelB);
            super.show();
        }
    }

    static class CalRec extends Record {
        private final SuggestBox desc = new SuggestBox();
        private final DoubleInput quantity = 
            new DoubleInput(c.quantity(), 0, null);
        private final ListBox unit = new ListBox();
        private final DoubleInput calPerUnit = 
            new DoubleInput(c.calPerUnit(), 0, null);

        CalRec(String title) {
            super(title);
            g.resizeRows(5);

            g.setText(1, 0, c.desc());
            g.setWidget(1, 1, desc);
            desc.addEventHandler(new SuggestionHandler() {
                public void onSuggestionSelected(SuggestionEvent se) {
                    String text = 
                        se.getSelectedSuggestion().getReplacementString();
                    int index = text.lastIndexOf(' ');
                    if (index < 0)
                        return;
                    String cpu = text.substring(index + 1);
                    text = text.substring(0, index);
                    index = text.lastIndexOf(' ');
                    if (index < 0)
                        return;
                    desc.setText(text.substring(0, index));
                    setListBox(unit, text.substring(index + 1));
                    calPerUnit.change(cpu);
                    quantity.setFocus(true);

                }
            });

            g.setText(2, 0, c.quantity());
            g.setWidget(2, 1, quantity);

            g.setText(3, 0, c.unit());
            g.setWidget(3, 1, unit);

            g.setText(4, 0, c.calPerUnit());
            g.setWidget(4, 1, calPerUnit);
        }

        void setModelDB(Model.DB mdb) {
            super.setModelDB(mdb);
            for (int i = 0; i < mdb.units.length; i++) {
                unit.addItem(mdb.units[i]);
            }
        }

        void accept() {
            if (!date.isValid() || !quantity.isValid() || !calPerUnit.isValid())
                return;
            Model.CalRec mcr = 
                new Model.CalRec(date.get(), desc.getText(), quantity.get(), 
                                 getListBox(unit), calPerUnit.get(), 
                                 mdb.suggest());
            apply(mcr);
        }

        void dismiss() {
            desc.setText("");
            quantity.set(0);
            setListBox(unit, null);
            unit.setItemSelected(0, true);
            calPerUnit.set(0);
            super.dismiss();
        }

        void init(int row) {
            super.init(row);
            Model.CalRec mcr = (Model.CalRec)mdb.get(row);
            desc.setText(mcr.desc);
            quantity.change(mcr.quantity);
            setListBox(unit, mcr.unit);
            calPerUnit.change(mcr.calPerUnit);
        }

        void show(boolean isNew) {
            MultiWordSuggestOracle mwso = 
                (MultiWordSuggestOracle)desc.getSuggestOracle();
            mwso.clear();
            mwso.addAll(mdb.suggest());
            super.show(isNew);
            desc.setFocus(true);
        }
    }

    static class WeightRec extends Record {
        private final DoubleInput weight = 
            new DoubleInput(c.weightRec(), 0, null);

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

        void show(boolean isNew) {
            super.show(isNew);
            weight.setFocus(true);
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
            addButton(createOK());
            addButton(createCancel());

            VerticalPanel vp = new VerticalPanel();
            Grid g = new Grid(6, 2);

            g.setText(0, 0, c.metabolismOpt());
            metabolism = new DoubleInput(c.metabolismOpt(), 
                                         Model.Options.metabolism, null);
            g.setWidget(0, 1, metabolism);

            g.setText(1, 0, c.goalWeight());
            goalWeight = new DoubleInput(c.goalWeight(), 
                                         Model.Options.goalWeight, null);
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
            history = new IntegerInput(c.historyDays(), 
                                       Model.Options.history, null);
            g.setWidget(3, 1, history);

            CheckBox cb = new CheckBox(c.console());
            cb.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    if (((CheckBox)sender).isChecked()) {Console.show();}
                    else {Console.hide();}
                }
            });
            g.setWidget(4, 1, cb);

            g.setText(5, 0, c.revision());
            String rev = "$Rev$";
            g.setText(5, 1, rev.substring(6, rev.length() - 2));

            g.setWidth("100%");
            vp.add(g);

            setContent(vp);
        }

        void accept() {
            if (!metabolism.isValid() ||
                !goalWeight.isValid() ||
                !history.isValid())
                return;
            if (history.get() < c.minimalHistory())
                history.set(c.minimalHistory());
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
                Totals.daysInRange = IntegerInput.format(daysInRange);
                Totals.caloriesIn = 
                    DoubleInput.format(caloriesIn / daysInRange);
                Totals.pACalories = 
                    DoubleInput.format(pACalories / daysInRange);
                double metabolism = Options.metabolism *
                    (weight.size() > 0 ? ((WeightRec)weight.last()).weight : 0);
                Totals.metabolism = DoubleInput.format(metabolism);
                netCalories = 
                    DoubleInput.format(caloriesIn - pACalories - metabolism);
                double behavioralWeight = (caloriesIn - pACalories) / 
                    Options.metabolism / daysInRange;
                if (Options.weightIsKilograms)
                    behavioralWeight /= POUNDS_PER_KILO;
                Totals.behavioralWeight = DoubleInput.format(behavioralWeight);
                double calsLeftToEat = Options.goalWeight * Options.metabolism;
                if (Options.weightIsKilograms)
                    calsLeftToEat *= POUNDS_PER_KILO;
                calsLeftToEat += (pACalories - caloriesIn) / daysInRange;
                Totals.calsLeftToEat = DoubleInput.format(calsLeftToEat);
            }
        }

        static class Options {
            static double metabolism = 10.0;
            static double goalWeight = 0.0;
            static boolean weightIsPounds = false;
            static boolean weightIsKilograms = true;
            static int history = c.minimalHistory();

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
            final String unit;
            final String calPerUnit;
            final double calories;
            final String caloriesStr;

            CalRec(Date date, String desc, double quantity, 
                   String unit, double calPerUnit, Collection suggest) {
                super(date);
                this.desc = desc;
                this.quantity = DoubleInput.format(quantity);
                this.unit = unit;
                this.calPerUnit = DoubleInput.format(calPerUnit);
                this.calories = quantity * calPerUnit;
                this.caloriesStr = DoubleInput.format(calories);
                suggest.add(desc + " " + unit + " " + calPerUnit);
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
                    return DoubleInput.format(weight / 
                                              (Options.weightIsKilograms ? 
                                               POUNDS_PER_KILO : 1));
                }
                return super.getField(index);
            }
        }

        static class DB implements Comparator {
            private final ArrayList al = new ArrayList();
            final int nCol;
            final String[] units;
            final Collection suggest;

            DB(int nCol, String[] units, Collection suggest) {
                this.nCol = nCol;
                this.units = units;
                this.suggest = suggest;
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

            public Object remove(int index) {
                return al.remove(index);
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

            Collection suggest() {
                return suggest;
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

        final static String[] foodUnits = c.foodUnits();
        final static String[] pAUnits = c.pAUnits();

        final static Collection foodSuggest = new HashSet();
        final static Collection pASuggest = new HashSet();

        final static DB food = new DB(3, foodUnits, foodSuggest);
        final static DB pA = new DB(3, pAUnits, pASuggest);
        final static DB weight = new DB(2, null, null);

        // Testing data
        static {
            Options.metabolism = 11.0;
            Options.goalWeight = 77.0;

            food.add(new CalRec(DateInput.today(), "pasta", 14.0, 
                                foodUnits[4], 20.0, foodSuggest));
            food.add(new CalRec(DateInput.previousDay(50), "pasta", 18.0, 
                                foodUnits[4], 20.0, foodSuggest));
            food.add(new CalRec(DateInput.previousDay(65), "bread", 8.0, 
                                foodUnits[0], 20.0, foodSuggest));
            food.add(new CalRec(DateInput.previousDay(95), "pasta", 28.0, 
                                foodUnits[4], 20.0, foodSuggest));

            pA.add(new CalRec(DateInput.today(), "walking", 0.5, 
                              pAUnits[0], 300.0, pASuggest));

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

