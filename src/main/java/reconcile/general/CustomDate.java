package reconcile.general;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Objects;

public class CustomDate {

private int day = 0;
private int month = 0;
private int year = -1;
private static DateFormat[] formats = null;
private static DateFormat[] yearlessFormats = null;

public CustomDate(int day, int month, int year) {
  this.day = day;
  this.month = month;
  this.year = year;
  if (formats == null) {
    createFormats();
  }
}

public CustomDate(int day, int month) {
  this.day = day;
  this.month = month;
  this.year = -1;
  if (formats == null) {
    createFormats();
  }
}

@Override
public boolean equals(Object o)
{
  if (!(o instanceof CustomDate)) return false;
  CustomDate another = (CustomDate) o;
  if (day != another.day) return false;
  if (month != another.month) return false;
  if (year != -1 && another.year != -1 && year != another.year) return false;
  return true;
}

/* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
@Override
public int hashCode()
{
  return Objects.hashCode(day, month, year);
}
@SuppressWarnings("deprecation")
public static CustomDate getDate(String s)
{
  // Special case, sept is not parsed properly
  s = s.replaceAll("Sept\\.", "Sep.").replace("Sept\\s", "Sep ");
  // System.out.println(s);
  if (formats == null) {
    createFormats();
  }
  for (DateFormat f : formats) {
    try {
      Date d = f.parse(s);
      if (d != null) return new CustomDate(d.getDate(), d.getMonth() + 1, d.getYear() + 1900);
    }
    catch (ParseException pe) {

    }
  }
  for (DateFormat f : yearlessFormats) {
    try {
      Date d = f.parse(s);
      if (d != null) return new CustomDate(d.getDate(), d.getMonth() + 1);
    }
    catch (ParseException pe) {

    }
  }
  Matcher m1 = Pattern.compile("(\\d\\d)[-\\/](\\d\\d)").matcher(s);
  if (m1.matches()) return new CustomDate(Integer.parseInt(m1.group(2)), Integer.parseInt(m1.group(1)));
  Matcher m2 = Pattern.compile("(\\d\\d)[-\\/](\\d\\d)[-\\/](\\d\\d)").matcher(s);
  if (m2.matches())
    return new CustomDate(Integer.parseInt(m2.group(2)), Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(3)));
  return null;
}

private static void createFormats()
{
  formats = new DateFormat[4];
  formats[0] = new SimpleDateFormat("MMM dd, yy");
  formats[1] = new SimpleDateFormat("MMM dd, yyyy");
  formats[2] = new SimpleDateFormat("MMM. dd, yy");
  formats[3] = new SimpleDateFormat("MMM. dd, yyyy");
  yearlessFormats = new DateFormat[2];
  yearlessFormats[0] = new SimpleDateFormat("MMM dd");
  yearlessFormats[1] = new SimpleDateFormat("MMM. dd");
}

@Override
public String toString()
{
  return "" + day + "." + month + (year > 0 ? "." + year : "");
}
}
