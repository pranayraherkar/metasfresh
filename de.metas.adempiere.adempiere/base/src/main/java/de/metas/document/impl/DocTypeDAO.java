package de.metas.document.impl;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.adempiere.ad.dao.ICompositeQueryFilter;
import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.dao.IQueryBuilder;
import org.adempiere.ad.dao.IQueryOrderBy.Direction;
import org.adempiere.ad.dao.IQueryOrderBy.Nulls;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.exceptions.DocTypeNotFoundException;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.adempiere.util.proxy.Cached;
import org.compiere.model.I_AD_Sequence;
import org.compiere.model.I_C_DocBaseType_Counter;
import org.compiere.model.I_C_DocType;
import org.compiere.model.MDocType;
import org.compiere.model.MSequence;
import org.compiere.util.Env;

import com.google.common.collect.ImmutableMap;

import de.metas.adempiere.util.CacheCtx;
import de.metas.adempiere.util.CacheTrx;
import de.metas.document.IDocTypeDAO;

public class DocTypeDAO implements IDocTypeDAO
{
	@Override
	public int getDocTypeIdOrNull(final Properties ctx, final String docBaseType, final int adClientId, final int adOrgId, final String trxName)
	{
		final String docSubType = DOCSUBTYPE_Any;
		return getDocTypeIdOrNull(ctx, docBaseType, docSubType, adClientId, adOrgId, trxName);
	}

	@Cached(cacheName = I_C_DocType.Table_Name + "#by#DocBaseType#DocSubType#AD_Client_ID#AD_Org_ID")
	public int getDocTypeIdOrNull(
			@CacheCtx final Properties ctx,
			final String docBaseType, final String docSubType, final int adClientId, final int adOrgId,
			@CacheTrx final String trxName)
	{
		final int docTypeId = createDocTypeByBaseTypeQuery(ctx, docBaseType, docSubType, adClientId, adOrgId, trxName)
				.create()
				.firstId();

		if (docTypeId <= 0)
		{
			return -1;
		}

		return docTypeId;
	}

	@Override
	public int getDocTypeId(final Properties ctx, final String docBaseType, final int adClientId, final int adOrgId, final String trxName)
	{
		final String docSubType = DOCSUBTYPE_Any;
		return getDocTypeIdOrNull(ctx, docBaseType, docSubType, adClientId, adOrgId, trxName);
	}

	@Override
	public int getDocTypeId(final Properties ctx, final String docBaseType, final String docSubType, final int adClientId, final int adOrgId, final String trxName)
	{
		final int docTypeId = getDocTypeIdOrNull(ctx, docBaseType, docSubType, adClientId, adOrgId, trxName);
		if (docTypeId <= 0)
		{
			final String additionalInfo = "@DocSubType@: " + docSubType
					+ ", @AD_Client_ID@: " + adClientId
					+ ", @AD_Org_ID@: " + adOrgId;
			throw new DocTypeNotFoundException(docBaseType, additionalInfo);
		}

		return docTypeId;
	}

	@Override
	public I_C_DocType getDocTypeOrNull(final Properties ctx,
			final String docBaseType,
			final int adClientId,
			final int adOrgId,
			final String trxName)
	{
		final String docSubType = DOCSUBTYPE_Any;
		return createDocTypeByBaseTypeQuery(ctx, docBaseType, docSubType, adClientId, adOrgId, trxName)
				.create()
				.first(I_C_DocType.class);
	}

	@Override
	public I_C_DocType getDocTypeOrNullForSOTrx(
			final Properties ctx,
			final String docBaseType,
			final String docSubType,
			final boolean isSOTrx,
			final int adClientId,
			final int adOrgId,
			final String trxName)
	{
		return createDocTypeByBaseTypeQuery(ctx, docBaseType, docSubType, adClientId, adOrgId, trxName)
				.addEqualsFilter(I_C_DocType.COLUMN_IsSOTrx, isSOTrx)
				.create().first(I_C_DocType.class);
	}

	@Override
	public I_C_DocType getDocType(
			final String docBaseType,
			final String docSubType,
			final int adClientId,
			final int adOrgId)
	{
		final I_C_DocType docType = getDocTypeOrNull(Env.getCtx(), docBaseType, docSubType, adClientId, adOrgId, ITrx.TRXNAME_None);
		if (docType == null)
		{
			final String additionalInfo = "@DocSubType@: " + docSubType
					+ ", @AD_Client_ID@: " + adClientId
					+ ", @AD_Org_ID@: " + adOrgId;
			throw new DocTypeNotFoundException(docBaseType, additionalInfo);
		}
		return docType;
	}

	@Override
	public I_C_DocType getDocTypeOrNull(final Properties ctx,
			final String docBaseType,
			final String docSubType,
			final int adClientId,
			final int adOrgId,
			final String trxName)
	{
		return createDocTypeByBaseTypeQuery(ctx, docBaseType, docSubType, adClientId, adOrgId, trxName)
				.create()
				.first(I_C_DocType.class);
	}

	private IQueryBuilder<I_C_DocType> createDocTypeByBaseTypeQuery(
			final Properties ctx,
			final String docBaseType,
			final String docSubType,
			final int adClientId,
			final int adOrgId,
			final String trxName)
	{
		Check.assumeNotNull(docBaseType, "docBaseType not null");

		final IQueryBuilder<I_C_DocType> queryBuilder = Services.get(IQueryBL.class)
				.createQueryBuilder(I_C_DocType.class, ctx, trxName);

		final ICompositeQueryFilter<I_C_DocType> filters = queryBuilder.getFilters();
		filters.addOnlyActiveRecordsFilter();
		filters.addEqualsFilter(I_C_DocType.COLUMNNAME_AD_Client_ID, adClientId);
		filters.addInArrayOrAllFilter(I_C_DocType.COLUMNNAME_AD_Org_ID, 0, adOrgId);
		filters.addEqualsFilter(I_C_DocType.COLUMNNAME_DocBaseType, docBaseType);

		if (docSubType == DOCSUBTYPE_NONE)
		{
			filters.addEqualsFilter(I_C_DocType.COLUMNNAME_DocSubType, null);
		}

		else if (docSubType != DOCSUBTYPE_Any)
		{
			filters.addEqualsFilter(I_C_DocType.COLUMNNAME_DocSubType, docSubType);
		}

		queryBuilder.orderBy()
				.addColumn(I_C_DocType.COLUMNNAME_IsDefault, Direction.Descending, Nulls.Last)
				.addColumn(I_C_DocType.COLUMNNAME_AD_Org_ID, Direction.Descending, Nulls.Last);

		return queryBuilder;
	}

	@Override
	public List<I_C_DocType> retrieveDocTypesByBaseType(final Properties ctx, final String docBaseType, final int adClientId, final int adOrgId, final String trxName)
	{
		final String docSubType = DOCSUBTYPE_Any;
		return createDocTypeByBaseTypeQuery(ctx, docBaseType, docSubType, adClientId, adOrgId, trxName)
				.create()
				.list(I_C_DocType.class);
	}

	@Override
	public String retrieveDocBaseTypeCounter(final Properties ctx, final String docBaseType)
	{
		final Map<String, String> docBaseTypePairs = retrieveDocBaseTypeCountersMap(ctx);

		return docBaseTypePairs.get(docBaseType);
	}

	@Cached(cacheName = I_C_DocBaseType_Counter.Table_Name)
	public Map<String, String> retrieveDocBaseTypeCountersMap(@CacheCtx final Properties ctx)
	{
		// load the existing info from the table C_DocBaseType_Counter in an immutable map
		ImmutableMap.Builder<String, String> docBaseTypeCounters = ImmutableMap.builder();

		final IQueryBuilder<I_C_DocBaseType_Counter> queryBuilder = Services.get(IQueryBL.class).createQueryBuilder(I_C_DocBaseType_Counter.class, ctx, ITrx.TRXNAME_None);

		queryBuilder.addOnlyActiveRecordsFilter()
				.addOnlyContextClient();

		final List<I_C_DocBaseType_Counter> docBaseTypeCountersList = queryBuilder.create().list();

		for (final I_C_DocBaseType_Counter docBaseTypeCounter : docBaseTypeCountersList)
		{
			docBaseTypeCounters.put(docBaseTypeCounter.getDocBaseType(), docBaseTypeCounter.getCounter_DocBaseType());
		}

		return docBaseTypeCounters.build();
	}

	@Override
	public I_C_DocType createDocType(final DocTypeCreateRequest request)
	{
		final Properties ctx = request.getCtx();
		final String trxName = ITrx.TRXNAME_ThreadInherited;
		final String name = request.getName();

		final int docNoSequenceId;
		if(request.getDocNoSequenceId() > 0)
		{
			docNoSequenceId = request.getDocNoSequenceId();
		}
		else if(request.getNewDocNoSequenceStartNo() > 0)
		{
			final I_AD_Sequence sequence = new MSequence(ctx, Env.getAD_Client_ID(ctx), name, request.getNewDocNoSequenceStartNo(), trxName);
			InterfaceWrapperHelper.save(sequence);
			docNoSequenceId = sequence.getAD_Sequence_ID();
		}
		else
		{
			docNoSequenceId = -1;
		}

		final MDocType dt = new MDocType(ctx, request.getDocBaseType(), name, trxName);
		dt.setEntityType(request.getEntityType());
		if(request.getAdOrgId() > 0)
		{
			dt.setAD_Org_ID(request.getAdOrgId());
		}
		if (request.getPrintName() != null && request.getPrintName().length() > 0)
		{
			dt.setPrintName(request.getPrintName()); // Defaults to Name
		}
		if (request.getDocSubType() != null)
		{
			dt.setDocSubType(request.getDocSubType());
		}
		if (request.getDocTypeShipmentId() > 0)
		{
			dt.setC_DocTypeShipment_ID(request.getDocTypeShipmentId());
		}
		if (request.getDocTypeInvoiceId() > 0)
		{
			dt.setC_DocTypeInvoice_ID(request.getDocTypeInvoiceId());
		}
		if (request.getGlCategoryId() > 0)
		{
			dt.setGL_Category_ID(request.getGlCategoryId());
		}
		
		if (docNoSequenceId <= 0)
		{
			dt.setIsDocNoControlled(false);
		}
		else
		{
			dt.setIsDocNoControlled(true);
			dt.setDocNoSequence_ID(docNoSequenceId);
		}
		
		if(request.getDocumentCopies() > 0)
		{
			dt.setDocumentCopies(request.getDocumentCopies());
		}

		if(request.getIsSOTrx() != null)
		{
			dt.setIsSOTrx(request.getIsSOTrx());
		}
		else
		{
			dt.setIsSOTrx();
		}
		
		InterfaceWrapperHelper.save(dt);
		return dt;
	}
}
