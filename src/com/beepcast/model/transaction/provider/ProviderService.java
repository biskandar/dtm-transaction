package com.beepcast.model.transaction.provider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.beepcast.api.provider.ProviderApp;
import com.beepcast.api.provider.util.RandomPriorityProviderGenerator;
import com.beepcast.dbmanager.common.CountryToProviderCommon;
import com.beepcast.dbmanager.common.OutgoingNumberToProviderCommon;
import com.beepcast.dbmanager.common.ProviderCommon;
import com.beepcast.dbmanager.table.TCountryToProvider;
import com.beepcast.dbmanager.table.TOutgoingNumberToProvider;
import com.beepcast.dbmanager.table.TOutgoingNumberToProviders;
import com.beepcast.dbmanager.table.TProvider;
import com.beepcast.model.transaction.TransactionLog;
import com.beepcast.oproperties.OnlinePropertiesApp;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class ProviderService {

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constanta
  //
  // //////////////////////////////////////////////////////////////////////////

  static final DLogContext lctx = new SimpleContext( "ProviderService" );

  // //////////////////////////////////////////////////////////////////////////
  //
  // Data Member
  //
  // //////////////////////////////////////////////////////////////////////////

  private OnlinePropertiesApp opropsApp;
  private ProviderApp providerApp;

  private TransactionLog log;

  // //////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  // //////////////////////////////////////////////////////////////////////////

  public ProviderService( TransactionLog transLog ) {

    opropsApp = OnlinePropertiesApp.getInstance();
    providerApp = ProviderApp.getInstance();

    this.log = transLog;

  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Support Function
  //
  // //////////////////////////////////////////////////////////////////////////

  public List listActiveProviders() {
    List lapids = new ArrayList();
    try {
      lapids.addAll( providerApp.listOutgoingProviderIds() );
    } catch ( Exception e ) {
      log.warning( "Failed to read list active providers , " + e );
    }
    return lapids;
  }

  public boolean isValidProviderId( String providerId , List lapids ) {
    boolean result = false;

    // validate exist provider id
    if ( StringUtils.isBlank( providerId ) ) {
      log.warning( "Found invalid provider id , found blank provider id" );
      return result;
    }

    // read and verify list active provider ids
    lapids = ( lapids == null ) ? listActiveProviders() : lapids;
    if ( ( lapids == null ) || ( lapids.size() < 1 ) ) {
      log.warning( "Found invalid provider id , found empty "
          + "list of active provider ids" );
      return result;
    }

    // verify is provider id is in the list active provider ids
    if ( lapids.indexOf( providerId ) < 0 ) {
      log.warning( "Found invalid provider id , found provider " + providerId
          + " is not in the active list" );
      return result;
    }

    result = true;
    return result;
  }

  public TProvider getProviderFromShortCode( String shortCode ) {
    TProvider providerOut = null;
    if ( StringUtils.isBlank( shortCode ) ) {
      return providerOut;
    }
    providerOut = ProviderCommon.getProviderFromShortCode( shortCode );
    return providerOut;
  }

  public TCountryToProvider getProviderFromCountry( String countryCode ) {
    TCountryToProvider ctp = null;
    if ( StringUtils.isBlank( countryCode ) ) {
      return ctp;
    }
    ctp = CountryToProviderCommon
        .getCountryToProviderByCountryCode( countryCode );
    return ctp;
  }

  public TOutgoingNumberToProvider getProviderFromLongNumberCountryCodeTelcoCodesGroupConnection(
      String headerLog , String longNumber , String countryCode ,
      String prefixNumber , List listTelcoCodes , int groupConnectionId ,
      List listProhibitProviderIds , List listCandidateProviderIds ) {
    TOutgoingNumberToProvider ontp = null;

    // capture delta time
    long deltaTime = System.currentTimeMillis();

    // prepare leveling
    int idxLevel , maxLevel = (int) opropsApp.getLong(
        "Transaction.MaxLevelFindGroupProviders" , 0 );

    // log it first
    log.debug( headerLog + "Resolving provider from the group providers "
        + ", based on : longNumber = " + longNumber + " , countryCode = "
        + countryCode + " , listTelcoCodes = " + listTelcoCodes
        + " , groupConnectionId = " + groupConnectionId
        + " , listProhibitProviderIds = " + listProhibitProviderIds
        + " , maxLevel = " + maxLevel );

    // header log
    String headerLogTmp = headerLog;

    // create clean list active provider ids
    List listActiveProviderIds = new ArrayList();
    listActiveProviderIds.addAll( listActiveProviders() );
    if ( ( listProhibitProviderIds != null )
        && ( listProhibitProviderIds.size() > 0 ) ) {
      listActiveProviderIds.removeAll( listProhibitProviderIds );
    }
    log.debug( headerLog + "Resolved list active provider ids : "
        + listActiveProviderIds );

    // go thru all the level until it hits the max
    for ( idxLevel = 0 ; idxLevel < maxLevel ; idxLevel = idxLevel + 1 ) {

      // header log
      headerLog = headerLogTmp + "[Level-" + idxLevel + "] ";

      // get group providers based on long number , level , country code
      // , telco code , and group connection
      TOutgoingNumberToProviders ontps = getGroupProviders( headerLog ,
          longNumber , idxLevel , countryCode , prefixNumber , listTelcoCodes ,
          groupConnectionId , listActiveProviderIds );
      if ( ( ontps == null ) || ( ontps.sizeRecords() < 1 ) ) {
        log.warning( headerLog + "Failed to resolve the group providers "
            + ", go to the next level" );
        continue;
      }

      // capture the list candidate provider ids for debug purpose
      if ( ontps != null ) {
        listCandidateProviderIds.clear();
        listCandidateProviderIds.addAll( extractListProviderIds( ontps ) );
      }

      // log it
      log.debug( headerLog + "Resolved group providers with : total = "
          + ontps.sizeRecords() + " provider(s) : "
          + extractListProviderIds( ontps ) );

      // get provider from group providers
      ontp = getProviderFromGroupProviders( headerLog , ontps ,
          listActiveProviderIds );
      if ( ontp != null ) {
        // already resolved the provider , stop process here
        break;
      }

    } // for ( idxLevel = 0 ; idxLevel < maxLevel ; idxLevel = idxLevel + 1 )

    // calculate delta time
    deltaTime = System.currentTimeMillis() - deltaTime;

    // log it if succeed
    if ( ontp != null ) {
      log.debug( headerLog + "Successfully resolved provider : id = "
          + ontp.getId() + " , providerId = " + ontp.getProviderId()
          + " , take " + deltaTime + " ms" );
    }

    return ontp;
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Core Function
  //
  // //////////////////////////////////////////////////////////////////////////

  private TOutgoingNumberToProvider getProviderFromGroupProviders(
      String headerLog , TOutgoingNumberToProviders ontps ,
      List listActiveProviderIds ) {
    TOutgoingNumberToProvider ontp = null;

    if ( ontps == null ) {
      return ontp;
    }

    // header log
    headerLog = ( headerLog == null ) ? "" : headerLog;

    // filter the map based on list active providers
    ontps = cleanOutgoingNumberToProviders( ontps , listActiveProviderIds );

    // prepare candidate provider
    TOutgoingNumberToProvider ontpFirst = null , ontpWinner = null;

    // get first candidate from the map as validation as well
    ontpFirst = getFirstOutgoingNumberToProvider( ontps );
    if ( ontpFirst == null ) {
      log.warning( headerLog + "Failed to get provider "
          + ", found empty list of map outgoing number to provider" );
      return ontp;
    }

    // get candidate provider with random priority
    ontpWinner = getCandidateWinnerFromRandomPriority( headerLog , ontps ,
        listActiveProviderIds );
    if ( ontpWinner != null ) {
      log.debug( headerLog + "Found provider winner from random priority : "
          + "providerId = " + ontpWinner.getProviderId() );
      ontp = ontpWinner;
      return ontp;
    }

    // get candidate provider with sequence priority
    ontpWinner = getCandidateWinnerFromSequencePriority( headerLog , ontps ,
        listActiveProviderIds );
    if ( ontpWinner != null ) {
      log.debug( headerLog + "Found provider winner from sequence priority : "
          + "providerId = " + ontpWinner.getProviderId() );
      ontp = ontpWinner;
      return ontp;
    }

    // get candidate provider from the first index available providers
    log.debug( headerLog + "Defined provider winner from first candidate : "
        + "providerId = " + ontpFirst.getProviderId() );
    ontp = ontpFirst;
    return ontp;
  }

  private List extractListProviderIds( TOutgoingNumberToProviders ontps ) {
    List listProviderIds = null;
    if ( ontps == null ) {
      return listProviderIds;
    }
    listProviderIds = new ArrayList();
    TOutgoingNumberToProvider ontp = null;
    Iterator iterRecords = ontps.iterRecords();
    while ( iterRecords.hasNext() ) {
      ontp = (TOutgoingNumberToProvider) iterRecords.next();
      if ( ontp == null ) {
        continue;
      }
      listProviderIds.add( ontp.getProviderId() );
    }
    return listProviderIds;
  }

  private TOutgoingNumberToProviders getGroupProviders( String headerLog ,
      String longNumber , int level , String countryCode , String prefixNumber ,
      List listTelcoCodes , int groupConnectionId , List listActiveProviderIds ) {
    TOutgoingNumberToProviders ontps = null;

    // header log

    headerLog = ( headerLog == null ) ? "" : headerLog;

    // validate must be params

    if ( groupConnectionId < 1 ) {
      log.warning( headerLog + "Failed to resolve group providers "
          + ", found invalid groupConnectionId = " + groupConnectionId );
      return ontps;
    }
    if ( level < 0 ) {
      log.warning( headerLog + "Failed to resolve group providers "
          + ", found invalid level = " + level );
      return ontps;
    }

    // resolving group providers

    try {

      // when country code not exists

      if ( StringUtils.isBlank( countryCode ) ) {
        log.debug( headerLog + "Resolving group providers by : level = "
            + level + " , groupConnectionId = " + groupConnectionId );
        ontps = getGroupProviders( headerLog , longNumber , level , "*" , "*" ,
            "*" , groupConnectionId , listActiveProviderIds );
        return ontps;
      }

      // when prefix number not exists

      if ( StringUtils.isBlank( prefixNumber ) ) {
        log.debug( headerLog + "Resolving group providers by : level = "
            + level + " , groupConnectionId = " + groupConnectionId
            + " , countryCode = " + countryCode
            + " , prefixNumber = * , telcoCode = * " );
        ontps = getGroupProviders( headerLog , longNumber , level ,
            countryCode , "*" , "*" , groupConnectionId , listActiveProviderIds );
        if ( ( ontps == null ) || ( ontps.sizeRecords() < 1 ) ) {
          log.debug( headerLog + "Resolving group providers by : level = "
              + level + " , groupConnectionId = " + groupConnectionId
              + " , countryCode = * , prefixNumber = * , telcoCode = * " );
          ontps = getGroupProviders( headerLog , longNumber , level , "*" ,
              "*" , "*" , groupConnectionId , listActiveProviderIds );
        }
        return ontps;
      }

      // when list telco codes empty

      if ( ( listTelcoCodes == null ) || ( listTelcoCodes.size() < 1 ) ) {
        if ( ( ontps == null ) || ( ontps.sizeRecords() < 1 ) ) {
          log.debug( headerLog + "Resolving group providers : level = " + level
              + " , groupConnectionId = " + groupConnectionId
              + " , countryCode = " + countryCode + " , prefixNumber = "
              + prefixNumber + " , telcoCode = * " );
          ontps = getGroupProviders( headerLog , longNumber , level ,
              countryCode , prefixNumber , "*" , groupConnectionId ,
              listActiveProviderIds );
        }
        if ( ( ontps == null ) || ( ontps.sizeRecords() < 1 ) ) {
          log.debug( headerLog + "Resolving group providers : level = " + level
              + " , groupConnectionId = " + groupConnectionId
              + " , countryCode = * , prefixNumber = " + prefixNumber
              + " , telcoCode = * " );
          ontps = getGroupProviders( headerLog , longNumber , level , "*" ,
              prefixNumber , "*" , groupConnectionId , listActiveProviderIds );
        }
        if ( ( ontps == null ) || ( ontps.sizeRecords() < 1 ) ) {
          log.debug( headerLog + "Resolving group providers by : level = "
              + level + " , groupConnectionId = " + groupConnectionId
              + " , countryCode = " + countryCode
              + " , prefixNumber = * , telcoCode = * " );
          ontps = getGroupProviders( headerLog , longNumber , level ,
              countryCode , "*" , "*" , groupConnectionId ,
              listActiveProviderIds );
        }
        if ( ( ontps == null ) || ( ontps.sizeRecords() < 1 ) ) {
          log.debug( headerLog + "Resolving group providers by : level = "
              + level + " , groupConnectionId = " + groupConnectionId
              + " , countryCode = * , prefixNumber = * , telcoCode = * " );
          ontps = getGroupProviders( headerLog , longNumber , level , "*" ,
              "*" , "*" , groupConnectionId , listActiveProviderIds );
        }
        return ontps;
      }

      // validate for all

      log.debug( headerLog + "Resolving group providers : level = " + level
          + " , groupConnectionId = " + groupConnectionId + " , countryCode = "
          + countryCode + " , listTelcoCodes = " + listTelcoCodes );
      Iterator iterTelcoCodes = listTelcoCodes.iterator();
      while ( iterTelcoCodes.hasNext() ) {
        String telcoCode = (String) iterTelcoCodes.next();
        if ( StringUtils.isBlank( telcoCode ) ) {
          continue;
        }
        if ( ( ontps == null ) || ( ontps.sizeRecords() < 1 ) ) {
          log.debug( headerLog + "Resolving group providers : level = " + level
              + " , groupConnectionId = " + groupConnectionId
              + " , countryCode = " + countryCode + " , prefixNumber = "
              + prefixNumber + " , telcoCode = " + telcoCode );
          ontps = getGroupProviders( headerLog , longNumber , level ,
              countryCode , prefixNumber , telcoCode , groupConnectionId ,
              listActiveProviderIds );
        }
        if ( ( ontps == null ) || ( ontps.sizeRecords() < 1 ) ) {
          log.debug( headerLog + "Resolving group providers : level = " + level
              + " , groupConnectionId = " + groupConnectionId
              + " , countryCode = " + countryCode
              + " , prefixNumber = * , telcoCode = " + telcoCode );
          ontps = getGroupProviders( headerLog , longNumber , level ,
              countryCode , "*" , telcoCode , groupConnectionId ,
              listActiveProviderIds );
        }
        if ( ( ontps == null ) || ( ontps.sizeRecords() < 1 ) ) {
          log.debug( headerLog + "Resolving group providers : level = " + level
              + " , groupConnectionId = " + groupConnectionId
              + " , countryCode = * , prefixNumber = * , telcoCode = "
              + telcoCode );
          ontps = getGroupProviders( headerLog , longNumber , level , "*" ,
              "*" , telcoCode , groupConnectionId , listActiveProviderIds );
        }
        if ( ( ontps != null ) && ( ontps.sizeRecords() > 0 ) ) {
          break;
        }
      }
      if ( ( ontps == null ) || ( ontps.sizeRecords() < 1 ) ) {
        log.debug( headerLog + "Resolving group providers : level = " + level
            + " , groupConnectionId = " + groupConnectionId
            + " , countryCode = " + countryCode + " , prefixNumber = "
            + prefixNumber + " , telcoCode = *" );
        ontps = getGroupProviders( headerLog , longNumber , level ,
            countryCode , prefixNumber , "*" , groupConnectionId ,
            listActiveProviderIds );
      }
      if ( ( ontps == null ) || ( ontps.sizeRecords() < 1 ) ) {
        log.debug( headerLog + "Resolving group providers : level = " + level
            + " , groupConnectionId = " + groupConnectionId
            + " , countryCode = " + countryCode
            + " , prefixNumber = * , telcoCode = *" );
        ontps = getGroupProviders( headerLog , longNumber , level ,
            countryCode , "*" , "*" , groupConnectionId , listActiveProviderIds );
      }
      if ( ( ontps == null ) || ( ontps.sizeRecords() < 1 ) ) {
        log.debug( headerLog + "Resolving group providers by : level = "
            + level + " , groupConnectionId = " + groupConnectionId
            + " , countryCode = * , prefixNumber = * , telcoCode = * " );
        ontps = getGroupProviders( headerLog , longNumber , level , "*" , "*" ,
            "*" , groupConnectionId , listActiveProviderIds );
      }

    } catch ( Exception e ) {
      log.warning( headerLog + "Failed to resolve group providers , " + e );
    }

    return ontps;
  }

  private TOutgoingNumberToProviders getGroupProviders( String headerLog ,
      String longNumber , int level , String countryCode , String prefixNumber ,
      String telcoCode , int groupConnectionId , List listActiveProviderIds ) {
    TOutgoingNumberToProviders ontps = OutgoingNumberToProviderCommon
        .getOutgoingNumberToProviders( longNumber , level , countryCode ,
            prefixNumber , telcoCode , groupConnectionId );

    if ( ontps == null ) {
      return ontps;
    }

    int sizeRecords = ontps.sizeRecords();
    if ( sizeRecords < 1 ) {
      ontps = null;
      return ontps;
    }

    int totalActive = 0;
    String ontpProviderId = null;
    TOutgoingNumberToProvider ontp = null;
    Iterator iterRecords = ontps.iterRecords();
    while ( iterRecords.hasNext() ) {
      ontp = (TOutgoingNumberToProvider) iterRecords.next();
      if ( ontp == null ) {
        continue;
      }
      ontpProviderId = ontp.getProviderId();
      if ( ontpProviderId == null ) {
        continue;
      }

      if ( ( listActiveProviderIds != null )
          && ( listActiveProviderIds.indexOf( ontpProviderId ) > -1 ) ) {
        totalActive = totalActive + 1;
      }

    } // while ( iterRecords.hasNext() )

    if ( ( ontps != null ) && ( totalActive < 1 ) ) {
      ontps = null;
    }

    return ontps;
  }

  private TOutgoingNumberToProviders cleanOutgoingNumberToProviders(
      TOutgoingNumberToProviders ontpsIn , List lapids ) {
    TOutgoingNumberToProviders ontpsOut = null;

    if ( ontpsIn == null ) {
      return ontpsOut;
    }

    if ( lapids == null ) {
      return ontpsOut;
    }

    List listIds = ontpsIn.getIds();
    if ( listIds == null ) {
      return ontpsOut;
    }

    ontpsOut = new TOutgoingNumberToProviders();
    List ilpids = new ArrayList() , vlpids = new ArrayList();
    Iterator iterIds = listIds.iterator();
    while ( iterIds.hasNext() ) {
      String idx = (String) iterIds.next();
      if ( idx == null ) {
        continue;
      }
      TOutgoingNumberToProvider ontpIdx = ontpsIn
          .getOutgoingNumberToProvider( idx );
      if ( ontpIdx == null ) {
        continue;
      }
      String providerId = ontpIdx.getProviderId();
      if ( StringUtils.isBlank( providerId ) ) {
        continue;
      }
      if ( lapids.indexOf( providerId ) < 0 ) {
        ilpids.add( providerId );
        continue;
      }
      if ( !ontpsOut.addOutgoingNumberToProvider( ontpIdx ) ) {
        ilpids.add( providerId );
        continue;
      }
      vlpids.add( providerId );
    }

    // log it
    log.debug( "Clean list providers read from "
        + "outgoingNumberToProvider table : validList = " + vlpids
        + " , invalidList = " + ilpids );

    return ontpsOut;
  }

  private TOutgoingNumberToProvider getFirstOutgoingNumberToProvider(
      TOutgoingNumberToProviders ontps ) {
    TOutgoingNumberToProvider ontp = null;

    List listIds = ontps.getIds();
    if ( listIds == null ) {
      return ontp;
    }

    String idx = null;
    Iterator iterIds = listIds.iterator();
    if ( iterIds.hasNext() ) {
      idx = (String) iterIds.next();
    }

    if ( idx == null ) {
      return ontp;
    }

    ontp = ontps.getOutgoingNumberToProvider( idx );

    return ontp;
  }

  private TOutgoingNumberToProvider getCandidateWinnerFromRandomPriority(
      String headerLog , TOutgoingNumberToProviders ontps , List lapids ) {
    TOutgoingNumberToProvider ontpResult = null;

    // header log
    headerLog = ( headerLog == null ) ? "" : headerLog;

    // list provider ids
    List ids = ontps.getIds();
    if ( ids == null ) {
      return ontpResult;
    }

    // prepare random engine
    RandomPriorityProviderGenerator randEngine = new RandomPriorityProviderGenerator();

    // setup list random priority providers
    int totalProviders = 0;
    Iterator iterIds = ids.iterator();
    while ( iterIds.hasNext() ) {
      String idx = (String) iterIds.next();
      if ( idx == null ) {
        continue;
      }
      TOutgoingNumberToProvider ontpIdx = ontps
          .getOutgoingNumberToProvider( idx );
      if ( ontpIdx == null ) {
        continue;
      }
      int priority = ontpIdx.getPriority();
      if ( priority < 1 ) {
        continue;
      }
      randEngine.addProvider( priority , idx );
      totalProviders = totalProviders + 1;
    }

    // validate total providers
    if ( totalProviders < 1 ) {
      log.warning( headerLog
          + "Failed to get candidate winner from random priority "
          + ", found zero total providers loaded into random list" );
      return ontpResult;
    }

    // get next random provider
    String idx = randEngine.getNextProviderId();
    if ( idx == null ) {
      log.warning( headerLog
          + "Failed to get candidate winner from random priority "
          + ", found null next rand value" );
      return ontpResult;
    }

    // get provider map bean from the map
    ontpResult = ontps.getOutgoingNumberToProvider( idx );
    if ( ontpResult == null ) {
      log.warning( headerLog
          + "Failed to get candidate winner from random priority "
          + ", found invalid map id" );
      return ontpResult;
    }

    // verify is the provider is valid ?
    if ( !isValidOutgoingNumberToProvider( ontpResult , lapids ) ) {
      log.warning( headerLog
          + "Failed to get candidate winner from random priority "
          + ", found invalid winner provider" );
      ontpResult = null;
      return ontpResult;
    }

    return ontpResult;
  }

  private TOutgoingNumberToProvider getCandidateWinnerFromSequencePriority(
      String headerLog , TOutgoingNumberToProviders ontps , List lapids ) {
    TOutgoingNumberToProvider ontpResult = null;

    // header log
    headerLog = ( headerLog == null ) ? "" : headerLog;

    // list provider ids
    List ids = ontps.getIds();
    if ( ids == null ) {
      return ontpResult;
    }

    // prepare random engine
    RandomPriorityProviderGenerator randEngine = new RandomPriorityProviderGenerator();

    // setup list random priority providers
    int totalProviders = 0;
    Iterator iterIds = ids.iterator();
    while ( iterIds.hasNext() ) {
      String idx = (String) iterIds.next();
      if ( idx == null ) {
        continue;
      }
      TOutgoingNumberToProvider ontpIdx = ontps
          .getOutgoingNumberToProvider( idx );
      if ( ontpIdx == null ) {
        continue;
      }
      // use same priority value across all available providers ~ 1
      randEngine.addProvider( 1 , idx );
      totalProviders = totalProviders + 1;
    }

    // validate total providers
    if ( totalProviders < 1 ) {
      log.warning( headerLog
          + "Failed to get candidate winner from sequence priority "
          + ", found zero total providers loaded into random list" );
      return ontpResult;
    }

    // get next random provider
    String idx = randEngine.getNextProviderId();
    if ( idx == null ) {
      log.warning( headerLog
          + "Failed to get candidate winner from sequence priority "
          + ", found null next rand value" );
      return ontpResult;
    }

    // get provider map bean from the map
    ontpResult = ontps.getOutgoingNumberToProvider( idx );
    if ( ontpResult == null ) {
      log.warning( headerLog
          + "Failed to get candidate winner from sequence priority "
          + ", found invalid map id" );
      return ontpResult;
    }

    // verify is the provider is valid ?
    if ( !isValidOutgoingNumberToProvider( ontpResult , lapids ) ) {
      log.warning( headerLog
          + "Failed to get candidate winner from sequence priority "
          + ", found invalid winner provider" );
      ontpResult = null;
      return ontpResult;
    }

    return ontpResult;
  }

  private boolean isValidOutgoingNumberToProvider(
      TOutgoingNumberToProvider ontp , List lapids ) {
    boolean result = false;
    if ( ontp == null ) {
      log.warning( "Found invalid map outgoing number to provider "
          + ", found null input param" );
      return result;
    }
    if ( !isValidProviderId( ontp.getProviderId() , lapids ) ) {
      log.warning( "Found invalid map outgoing number to provider "
          + ", found invalid provider id " + ontp.getProviderId() );
      return result;
    }
    result = true;
    return result;
  }

}
