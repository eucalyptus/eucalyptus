/*************************************************************************
 * Copyright 1998-2011 Brian Wellington (bwelling@xbill.org)
 * Copyright 2017 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.vm.dns;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Credibility;
import org.xbill.DNS.DClass;
import org.xbill.DNS.DNAMERecord;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Master;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.NameTooLongException;
import org.xbill.DNS.Options;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.Type;
import com.eucalyptus.util.dns.SetResponses;

/**
 *
 */
class NonExpiringCache extends Cache {

  private interface Element {
    public boolean expired();
    public int compareCredibility(int cred);
    public int getType();
  }

  private static int
  limitExpire(long ttl, long maxttl) {
      return Integer.MAX_VALUE;  // Essentially never expire.
  }

  private static class CacheRRset extends RRset implements Element {
    private static final long serialVersionUID = 5971755205903597024L;

    int credibility;
    int expire;

    public
    CacheRRset(Record rec, int cred, long maxttl) {
      super();
      this.credibility = cred;
      this.expire = limitExpire(rec.getTTL(), maxttl);
      addRR(rec);
    }

    public
    CacheRRset(RRset rrset, int cred, long maxttl) {
      super(rrset);
      this.credibility = cred;
      this.expire = limitExpire(rrset.getTTL(), maxttl);
    }

    public final boolean
    expired() {
      int now = (int)(System.currentTimeMillis() / 1000);
      return (now >= expire);
    }

    public final int
    compareCredibility(int cred) {
      return credibility - cred;
    }

    public String
    toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(super.toString());
      sb.append(" cl = ");
      sb.append(credibility);
      return sb.toString();
    }
  }

  private static class NegativeElement implements Element {
    int type;
    Name name;
    int credibility;
    int expire;

    public
    NegativeElement(Name name, int type, SOARecord soa, int cred,
                    long maxttl)
    {
      this.name = name;
      this.type = type;
      long cttl = 0;
      if (soa != null)
        cttl = soa.getMinimum();
      this.credibility = cred;
      this.expire = limitExpire(cttl, maxttl);
    }

    public int
    getType() {
      return type;
    }

    public final boolean
    expired() {
      int now = (int)(System.currentTimeMillis() / 1000);
      return (now >= expire);
    }

    public final int
    compareCredibility(int cred) {
      return credibility - cred;
    }

    public String
    toString() {
      StringBuffer sb = new StringBuffer();
      if (type == 0)
        sb.append("NXDOMAIN " + name);
      else
        sb.append("NXRRSET " + name + " " + Type.string(type));
      sb.append(" cl = ");
      sb.append(credibility);
      return sb.toString();
    }
  }

  private static class CacheMap extends LinkedHashMap {
    private int maxsize = -1;

    CacheMap(int maxsize) {
      super(16, (float) 0.75, true);
      this.maxsize = maxsize;
    }

    int
    getMaxSize() {
      return maxsize;
    }

    void
    setMaxSize(int maxsize) {
		/*
		 * Note that this doesn't shrink the size of the map if
		 * the maximum size is lowered, but it should shrink as
		 * entries expire.
		 */
      this.maxsize = maxsize;
    }

    protected boolean removeEldestEntry(Map.Entry eldest) {
      return maxsize >= 0 && size() > maxsize;
    }
  }

  private CacheMap data;
  private int maxncache = -1;
  private int maxcache = -1;
  private int dclass;

  private static final int defaultMaxEntries = 50000;

  /**
   * Creates an empty Cache
   *
   * @param dclass The DNS class of this cache
   * @see DClass
   */
  public
  NonExpiringCache(int dclass) {
    this.dclass = dclass;
    data = new CacheMap(defaultMaxEntries);
  }

  /**
   * Creates an empty Cache for class IN.
   * @see DClass
   */
  public
  NonExpiringCache() {
    this(DClass.IN);
  }

  /**
   * Creates a Cache which initially contains all records in the specified file.
   */
  public
  NonExpiringCache(String file) throws IOException {
    data = new CacheMap(defaultMaxEntries);
    Master m = new Master(file);
    Record record;
    while ((record = m.nextRecord()) != null)
      addRecord(record, Credibility.HINT, m);
  }

  private synchronized Object
  exactName(Name name) {
    return data.get(name);
  }

  private synchronized void
  removeName(Name name) {
    data.remove(name);
  }

  private synchronized Element []
  allElements(Object types) {
    if (types instanceof List ) {
      List typelist = (List) types;
      int size = typelist.size();
      return (Element []) typelist.toArray(new Element[size]);
    } else {
      Element set = (Element) types;
      return new Element[] {set};
    }
  }

  private synchronized Element
  oneElement(Name name, Object types, int type, int minCred) {
    Element found = null;

    if (type == Type.ANY)
      throw new IllegalArgumentException("oneElement(ANY)");
    if (types instanceof List) {
      List list = (List) types;
      for (int i = 0; i < list.size(); i++) {
        Element set = (Element) list.get(i);
        if (set.getType() == type) {
          found = set;
          break;
        }
      }
    } else {
      Element set = (Element) types;
      if (set.getType() == type)
        found = set;
    }
    if (found == null)
      return null;
    if (found.expired()) {
      removeElement(name, type);
      return null;
    }
    if (found.compareCredibility(minCred) < 0)
      return null;
    return found;
  }

  private synchronized Element
  findElement(Name name, int type, int minCred) {
    Object types = exactName(name);
    if (types == null)
      return null;
    return oneElement(name, types, type, minCred);
  }

  private synchronized void
  addElement(Name name, Element element) {
    Object types = data.get(name);
    if (types == null) {
      data.put(name, element);
      return;
    }
    int type = element.getType();
    if (types instanceof List) {
      List list = (List) types;
      for (int i = 0; i < list.size(); i++) {
        Element elt = (Element) list.get(i);
        if (elt.getType() == type) {
          list.set(i, element);
          return;
        }
      }
      list.add(element);
    } else {
      Element elt = (Element) types;
      if (elt.getType() == type)
        data.put(name, element);
      else {
        LinkedList list = new LinkedList();
        list.add(elt);
        list.add(element);
        data.put(name, list);
      }
    }
  }

  private synchronized void
  removeElement(Name name, int type) {
    Object types = data.get(name);
    if (types == null) {
      return;
    }
    if (types instanceof List) {
      List list = (List) types;
      for (int i = 0; i < list.size(); i++) {
        Element elt = (Element) list.get(i);
        if (elt.getType() == type) {
          list.remove(i);
          if (list.size() == 0)
            data.remove(name);
          return;
        }
      }
    } else {
      Element elt = (Element) types;
      if (elt.getType() != type)
        return;
      data.remove(name);
    }
  }

  /** Empties the Cache. */
  public synchronized void
  clearCache() {
    data.clear();
  }

  /**
   * Adds a record to the Cache.
   * @param r The record to be added
   * @param cred The credibility of the record
   * @param o The source of the record (this could be a Message, for example)
   * @see Record
   */
  public synchronized void
  addRecord(Record r, int cred, Object o) {
    Name name = r.getName();
    int type = r.getRRsetType();
    if (!Type.isRR(type))
      return;
    Element element = findElement(name, type, cred);
    if (element == null) {
      CacheRRset crrset = new CacheRRset(r, cred, maxcache);
      addRRset(crrset, cred);
    } else if (element.compareCredibility(cred) == 0) {
      if (element instanceof CacheRRset) {
        CacheRRset crrset = (CacheRRset) element;
        crrset.addRR(r);
      }
    }
  }

  /**
   * Adds an RRset to the Cache.
   * @param rrset The RRset to be added
   * @param cred The credibility of these records
   * @see RRset
   */
  public synchronized void
  addRRset(RRset rrset, int cred) {
    long ttl = rrset.getTTL();
    Name name = rrset.getName();
    int type = rrset.getType();
    Element element = findElement(name, type, 0);
    if (element != null && element.compareCredibility(cred) <= 0)
        element = null;
    if (element == null) {
        CacheRRset crrset;
        if (rrset instanceof CacheRRset)
            crrset = (CacheRRset) rrset;
        else
            crrset = new CacheRRset(rrset, cred, maxcache);
        addElement(name, crrset);
    }
  }

  /**
   * Adds a negative entry to the Cache.
   * @param name The name of the negative entry
   * @param type The type of the negative entry
   * @param soa The SOA record to add to the negative cache entry, or null.
   * The negative cache ttl is derived from the SOA.
   * @param cred The credibility of the negative entry
   */
  public synchronized void
  addNegative(Name name, int type, SOARecord soa, int cred) {
    long ttl = 0;
    if (soa != null)
      ttl = soa.getTTL();
    Element element = findElement(name, type, 0);
    if (element != null && element.compareCredibility(cred) <= 0)
        element = null;
    if (element == null)
        addElement(name, new NegativeElement(name, type,
                                             soa, cred,
                                             maxncache));
  }

  /**
   * Finds all matching sets or something that causes the lookup to stop.
   */
  protected synchronized SetResponse
  lookup(Name name, int type, int minCred) {
    int labels;
    int tlabels;
    Element element;
    Name tname;
    Object types;
    SetResponse sr;

    labels = name.labels();

    for (tlabels = labels; tlabels >= 1; tlabels--) {
      boolean isRoot = (tlabels == 1);
      boolean isExact = (tlabels == labels);

      if (isRoot)
        tname = Name.root;
      else if (isExact)
        tname = name;
      else
        tname = new Name(name, labels - tlabels);

      types = data.get(tname);
      if (types == null)
        continue;

		/*
		 * If this is the name, look for the actual type or a CNAME
		 * (unless it's an ANY query, where we return everything).
		 * Otherwise, look for a DNAME.
		 */
      if (isExact && type == Type.ANY) {
        sr = SetResponses.ofType( SetResponses.SetResponseType.successful );
        Element [] elements = allElements(types);
        int added = 0;
        for (int i = 0; i < elements.length; i++) {
          element = elements[i];
          if (element.expired()) {
            removeElement(tname, element.getType());
            continue;
          }
          if (!(element instanceof CacheRRset))
            continue;
          if (element.compareCredibility(minCred) < 0)
            continue;
          SetResponses.addRRset( sr, (CacheRRset)element);
          added++;
        }
			/* There were positive entries */
        if (added > 0)
          return sr;
      } else if (isExact) {
        element = oneElement(tname, types, type, minCred);
        if (element != null &&
            element instanceof CacheRRset)
        {
          sr = SetResponses.ofType( SetResponses.SetResponseType.successful);
          SetResponses.addRRset( sr, (CacheRRset) element);
          return sr;
        } else if (element != null) {
          sr = SetResponses.ofType( SetResponses.SetResponseType.nxrrset);
          return sr;
        }

        element = oneElement(tname, types, Type.CNAME, minCred);
        if (element != null &&
            element instanceof CacheRRset)
        {
          return SetResponses.newInstance( SetResponses.SetResponseType.cname,
              (CacheRRset) element);
        }
      } else {
        element = oneElement(tname, types, Type.DNAME, minCred);
        if (element != null &&
            element instanceof CacheRRset)
        {
          return SetResponses.newInstance( SetResponses.SetResponseType.dname,
              (CacheRRset) element);
        }
      }

		/* Look for an NS */
      element = oneElement(tname, types, Type.NS, minCred);
      if (element != null && element instanceof CacheRRset)
        return SetResponses.newInstance( SetResponses.SetResponseType.delegation,
            (CacheRRset) element);

		/* Check for the special NXDOMAIN element. */
      if (isExact) {
        element = oneElement(tname, types, 0, minCred);
        if (element != null)
          return SetResponses.ofType( SetResponses.SetResponseType.nxdomain);
      }

    }
    return SetResponses.ofType( SetResponses.SetResponseType.unknown);
  }

  /**
   * Looks up Records in the Cache.  This follows CNAMEs and handles negatively
   * cached data.
   * @param name The name to look up
   * @param type The type to look up
   * @param minCred The minimum acceptable credibility
   * @return A SetResponse object
   * @see SetResponse
   * @see Credibility
   */
  public SetResponse
  lookupRecords(Name name, int type, int minCred) {
    return lookup(name, type, minCred);
  }

  private RRset []
  findRecords(Name name, int type, int minCred) {
    SetResponse cr = lookupRecords(name, type, minCred);
    if (cr.isSuccessful())
      return cr.answers();
    else
      return null;
  }

  /**
   * Looks up credible Records in the Cache (a wrapper around lookupRecords).
   * Unlike lookupRecords, this given no indication of why failure occurred.
   * @param name The name to look up
   * @param type The type to look up
   * @return An array of RRsets, or null
   * @see Credibility
   */
  public RRset []
  findRecords(Name name, int type) {
    return findRecords(name, type, Credibility.NORMAL);
  }

  /**
   * Looks up Records in the Cache (a wrapper around lookupRecords).  Unlike
   * lookupRecords, this given no indication of why failure occurred.
   * @param name The name to look up
   * @param type The type to look up
   * @return An array of RRsets, or null
   * @see Credibility
   */
  public RRset []
  findAnyRecords(Name name, int type) {
    return findRecords(name, type, Credibility.GLUE);
  }

  private final int
  getCred(int section, boolean isAuth) {
    if (section == Section.ANSWER) {
      if (isAuth)
        return Credibility.AUTH_ANSWER;
      else
        return Credibility.NONAUTH_ANSWER;
    } else if (section == Section.AUTHORITY) {
      if (isAuth)
        return Credibility.AUTH_AUTHORITY;
      else
        return Credibility.NONAUTH_AUTHORITY;
    } else if (section == Section.ADDITIONAL) {
      return Credibility.ADDITIONAL;
    } else
      throw new IllegalArgumentException("getCred: invalid section");
  }

  private static void
  markAdditional(RRset rrset, Set names) {
    Record first = rrset.first();
    if (first.getAdditionalName() == null)
      return;

    Iterator it = rrset.rrs();
    while (it.hasNext()) {
      Record r = (Record) it.next();
      Name name = r.getAdditionalName();
      if (name != null)
        names.add(name);
    }
  }

  /**
   * Adds all data from a Message into the Cache.  Each record is added with
   * the appropriate credibility, and negative answers are cached as such.
   * @param in The Message to be added
   * @return A SetResponse that reflects what would be returned from a cache
   * lookup, or null if nothing useful could be cached from the message.
   * @see Message
   */
  public SetResponse
  addMessage(Message in) {
    boolean isAuth = in.getHeader().getFlag(Flags.AA);
    Record question = in.getQuestion();
    Name qname;
    Name curname;
    int qtype;
    int qclass;
    int cred;
    int rcode = in.getHeader().getRcode();
    boolean completed = false;
    RRset [] answers, auth, addl;
    SetResponse response = null;
    boolean verbose = Options.check("verbosecache");
    HashSet additionalNames;

    if ((rcode != Rcode.NOERROR && rcode != Rcode.NXDOMAIN) ||
        question == null)
      return null;

    qname = question.getName();
    qtype = question.getType();
    qclass = question.getDClass();

    curname = qname;

    additionalNames = new HashSet();

    answers = in.getSectionRRsets(Section.ANSWER);
    for (int i = 0; i < answers.length; i++) {
      if (answers[i].getDClass() != qclass)
        continue;
      int type = answers[i].getType();
      Name name = answers[i].getName();
      cred = getCred(Section.ANSWER, isAuth);
      if ((type == qtype || qtype == Type.ANY) &&
          name.equals(curname))
      {
        addRRset(answers[i], cred);
        completed = true;
        if (curname == qname) {
          if (response == null)
            response = SetResponses.ofType( SetResponses.SetResponseType.successful);
          SetResponses.addRRset( response, answers[i]);
        }
        markAdditional(answers[i], additionalNames);
      } else if (type == Type.CNAME && name.equals(curname)) {
        CNAMERecord cname;
        addRRset(answers[i], cred);
        if (curname == qname)
          response = SetResponses.newInstance( SetResponses.SetResponseType.cname,
              answers[i]);
        cname = (CNAMERecord) answers[i].first();
        curname = cname.getTarget();
      } else if (type == Type.DNAME && curname.subdomain(name)) {
        DNAMERecord dname;
        addRRset(answers[i], cred);
        if (curname == qname)
          response = SetResponses.newInstance( SetResponses.SetResponseType.dname,
              answers[i]);
        dname = (DNAMERecord) answers[i].first();
        try {
          curname = curname.fromDNAME(dname);
        }
        catch (NameTooLongException e) {
          break;
        }
      }
    }

    auth = in.getSectionRRsets(Section.AUTHORITY);
    RRset soa = null, ns = null;
    for (int i = 0; i < auth.length; i++) {
      if (auth[i].getType() == Type.SOA &&
          curname.subdomain(auth[i].getName()))
        soa = auth[i];
      else if (auth[i].getType() == Type.NS &&
          curname.subdomain(auth[i].getName()))
        ns = auth[i];
    }
    if (!completed) {
		/* This is a negative response or a referral. */
      int cachetype = (rcode == Rcode.NXDOMAIN) ? 0 : qtype;
      if (rcode == Rcode.NXDOMAIN || soa != null || ns == null) {
			/* Negative response */
        cred = getCred(Section.AUTHORITY, isAuth);
        SOARecord soarec = null;
        if (soa != null)
          soarec = (SOARecord) soa.first();
        addNegative(curname, cachetype, soarec, cred);
        if (response == null) {
          SetResponses.SetResponseType responseType;
          if (rcode == Rcode.NXDOMAIN)
            responseType = SetResponses.SetResponseType.nxdomain;
          else
            responseType = SetResponses.SetResponseType.nxrrset;
          response = SetResponses.ofType( responseType);
        }
			/* DNSSEC records are not cached. */
      } else {
			/* Referral response */
        cred = getCred(Section.AUTHORITY, isAuth);
        addRRset(ns, cred);
        markAdditional(ns, additionalNames);
        if (response == null)
          response = SetResponses.newInstance( SetResponses.SetResponseType.delegation,
              ns);
      }
    } else if (rcode == Rcode.NOERROR && ns != null) {
		/* Cache the NS set from a positive response. */
      cred = getCred(Section.AUTHORITY, isAuth);
      addRRset(ns, cred);
      markAdditional(ns, additionalNames);
    }

    addl = in.getSectionRRsets(Section.ADDITIONAL);
    for (int i = 0; i < addl.length; i++) {
      int type = addl[i].getType();
      if (type != Type.A && type != Type.AAAA && type != Type.A6)
        continue;
      Name name = addl[i].getName();
      if (!additionalNames.contains(name))
        continue;
      cred = getCred(Section.ADDITIONAL, isAuth);
      addRRset(addl[i], cred);
    }
    if (verbose)
      System.out.println("addMessage: " + response);
    return (response);
  }

  /**
   * Flushes an RRset from the cache
   * @param name The name of the records to be flushed
   * @param type The type of the records to be flushed
   * @see RRset
   */
  public void
  flushSet(Name name, int type) {
    removeElement(name, type);
  }

  /**
   * Flushes all RRsets with a given name from the cache
   * @param name The name of the records to be flushed
   * @see RRset
   */
  public void
  flushName(Name name) {
    removeName(name);
  }

  /**
   * Sets the maximum length of time that a negative response will be stored
   * in this Cache.  A negative value disables this feature (that is, sets
   * no limit).
   */
  public void
  setMaxNCache(int seconds) {
    maxncache = seconds;
  }

  /**
   * Gets the maximum length of time that a negative response will be stored
   * in this Cache.  A negative value indicates no limit.
   */
  public int
  getMaxNCache() {
    return maxncache;
  }

  /**
   * Sets the maximum length of time that records will be stored in this
   * Cache.  A negative value disables this feature (that is, sets no limit).
   */
  public void
  setMaxCache(int seconds) {
    maxcache = seconds;
  }

  /**
   * Gets the maximum length of time that records will be stored
   * in this Cache.  A negative value indicates no limit.
   */
  public int
  getMaxCache() {
    return maxcache;
  }

  /**
   * Gets the current number of entries in the Cache, where an entry consists
   * of all records with a specific Name.
   */
  public int
  getSize() {
    return data.size();
  }

  /**
   * Gets the maximum number of entries in the Cache, where an entry consists
   * of all records with a specific Name.  A negative value is treated as an
   * infinite limit.
   */
  public int
  getMaxEntries() {
    return data.getMaxSize();
  }

  /**
   * Sets the maximum number of entries in the Cache, where an entry consists
   * of all records with a specific Name.  A negative value is treated as an
   * infinite limit.
   *
   * Note that setting this to a value lower than the current number
   * of entries will not cause the Cache to shrink immediately.
   *
   * The default maximum number of entries is 50000.
   *
   * @param entries The maximum number of entries in the Cache.
   */
  public void
  setMaxEntries(int entries) {
    data.setMaxSize(entries);
  }

  /**
   * Returns the DNS class of this cache.
   */
  public int
  getDClass() {
    return dclass;
  }

  /**
   * Returns the contents of the Cache as a string.
   */
  public String
  toString() {
    StringBuffer sb = new StringBuffer();
    synchronized (this) {
      Iterator it = data.values().iterator();
      while (it.hasNext()) {
        Element [] elements = allElements(it.next());
        for (int i = 0; i < elements.length; i++) {
          sb.append(elements[i]);
          sb.append("\n");
        }
      }
    }
    return sb.toString();
  }
}
